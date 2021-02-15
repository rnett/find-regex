package com.rnett.action

import com.rnett.action.core.fail
import com.rnett.action.core.inputs
import com.rnett.action.core.log
import com.rnett.action.core.outputs
import com.rnett.action.exec.exec
import querystring.unescape

/**
 * ' -> literal
 * " -> normal
 *
 */
fun unQuote(command: String): String {
    if (command.startsWith('\'') && command.endsWith('\'')) {
        return command
    } else {
        return unescape(command.trim('"'))
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\'", "\'")
            .replace("\\\"", "\"")
    }
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun main() {
    val regexText by inputs("regex")

    val commands = inputs.getRequired("commands")
        .split(",")
        .filter(String::isNotBlank)
        .map(::unQuote)

    val files = inputs.getRequired("commands")
        .split(",")
        .filter(String::isNotBlank)
        .map(::unQuote)
        .map(::Path)

    val requireMatch = inputs.getRequired("require-match").toBoolean()

    val group = inputs.getRequired("group").toIntOrNull() ?: 0

    val ignoreCase = inputs.getRequired("ignore-case").toBoolean()

    val multiline = inputs.getRequired("multiline").toBoolean()

    val options = buildSet {
        if (ignoreCase)
            add(RegexOption.IGNORE_CASE)
        if (multiline)
            add(RegexOption.MULTILINE)
    }

    val regex = Regex(regexText, options)

    fun setOutput(match: MatchResult) {
        if (group == 0) {
            outputs["match"] = match.value
        } else {
            outputs["match"] = match.groupValues.getOrNull(group)
                ?: fail("No match group $group, check your regex")
        }
    }

    files.forEach {
        log.info("Trying file $it")
        regex.find(it.read())?.let {
            setOutput(it)
            return
        }
    }

    commands.forEach {
        log.info("Trying command $it")
        val output = exec.execCommandAndCapture(it, ignoreReturnCode = true).run { if(returnCode == 0) stdout else null }
        if(output != null) {
            regex.find(output)?.let {
                setOutput(it)
                return
            }
        }
    }

    if(requireMatch)
        fail("No match found")
    else {
        log.info("No match found")
        outputs["match"] = ""
    }
}