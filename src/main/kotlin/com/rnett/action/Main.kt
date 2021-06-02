package com.rnett.action

import com.rnett.action.core.*
import com.rnett.action.exec.exec
import com.rnett.action.glob.glob

/**
 * ' -> literal
 * " -> normal
 *
 */
fun unQuote(command: String): String {
    if (command.startsWith('\'') && command.endsWith('\'')) {
        return command
    } else {
        return command.trim('"')
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\'", "\'")
            .replace("\\\"", "\"")
            .replace(Regex("%(\\d+)")) { it.groupValues[1].toInt().toChar().toString() }
    }
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun main() = runOrFail{
    val regexText by inputs("regex")

    val commands = inputs["commands"]
        .split(",")
        .filter(String::isNotBlank)
        .map(::unQuote)

    val files = glob(
        inputs["files"]
            .split(",")
            .filter(String::isNotBlank)
            .map(::unQuote)
    )

    val requireMatch = inputs["require-match"].toBoolean()

    val group = inputs["group"].toIntOrNull() ?: 0

    val ignoreCase = inputs["ignore-case"].toBoolean()

    val multiline = inputs["multiline"].toBoolean()

    val options = buildSet {
        if (ignoreCase)
            add(RegexOption.IGNORE_CASE)
        if (multiline)
            add(RegexOption.MULTILINE)
    }

    val regex = Regex(regexText, options)

    log.info("Regex: $regex")

    var matchOutput by outputs("match")

    fun setOutput(match: MatchResult) {
        log.info("Found match ${match.value}")
        val out = if (group == 0) {
            match.value
        } else {
            match.groupValues.getOrNull(group)
                ?: fail("No match group $group, check your regex")
        }
        matchOutput = out
        log.info("Matching part: $out")
    }

    files.forEach {
        log.info("Trying file $it")
        if (it.exists) {
            if (it.isFile) {
                regex.find(it.read())?.let {
                    setOutput(it)
                    return
                }
            } else {
                log.info("File is directory (or otherwise not a file)")
            }
        } else {
            log.info("File does not exist")
        }
    }

    commands.forEach {
        log.info("Trying command $it")
        val output = exec.execCommandAndCapture(it, ignoreReturnCode = true)
        if (output.returnCode != 0) {
            log.info("Command failed with error code ${output.returnCode}")
        } else {
            regex.find(output.stdout)?.let {
                setOutput(it)
                return
            }
        }
    }

    if (requireMatch)
        fail("No match found")
    else {
        log.info("No match found")
        matchOutput = ""
    }
}