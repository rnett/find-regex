package com.rnett.action

import com.rnett.action.core.*
import com.rnett.action.delegates.ifNull
import com.rnett.action.delegates.isTrue
import com.rnett.action.delegates.map
import com.rnett.action.delegates.toBoolean
import com.rnett.action.delegates.toInt
import com.rnett.action.exec.exec
import com.rnett.action.glob.glob
import com.rnett.action.glob.globFlow
import kotlinx.coroutines.flow.collect

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
suspend fun main() = runAction{
    val regexText by inputs("regex")

    val commands by inputs.map {
        it.split(",")
            .filter(String::isNotBlank)
            .map(::unQuote)
    }

    val files by inputs.map {
        it.split(",")
            .filter(String::isNotBlank)
            .map(::unQuote)
    }

    val requireMatch by inputs.isTrue()

    val group by inputs.toInt().ifNull { 0 }

    val ignoreCase by inputs.isTrue()

    val multiline by inputs.isTrue()

    val options = buildSet {
        if (ignoreCase)
            add(RegexOption.IGNORE_CASE)
        if (multiline)
            add(RegexOption.MULTILINE)
    }

    val regex = Regex(regexText, options)

    logger.info("Regex: $regex")

    var matchOutput by outputs("match")
    var foundOutput = false

    fun setOutput(match: MatchResult) {
        logger.info("Found match ${match.value}")
        val out = if (group == 0) {
            match.value
        } else {
            match.groupValues.getOrNull(group)
                ?: fail("No match group $group, check your regex")
        }
        matchOutput = out
        foundOutput = true
        logger.info("Matching part: $out")
    }

    globFlow(files).collect {
        if(foundOutput)
            return@collect
        logger.info("Trying file $it")
        if (it.exists) {
            if (it.isFile) {
                regex.find(it.readText())?.let {
                    setOutput(it)
                    return@collect
                }
            } else {
                logger.info("File is directory (or otherwise not a file)")
            }
        } else {
            logger.info("File does not exist")
        }
    }

    if(foundOutput)
        return@runAction

    commands.forEach {
        logger.info("Trying command $it")
        val output = exec.execCommandAndCapture(it, ignoreReturnCode = true)
        if (output.returnCode != 0) {
            logger.info("Command failed with error code ${output.returnCode}")
        } else {
            regex.find(output.stdout)?.let {
                setOutput(it)
                return@runAction
            }
        }
    }

    if (requireMatch)
        fail("No match found")
    else {
        logger.info("No match found")
        matchOutput = ""
    }
}