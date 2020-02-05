package utils

object SlackJsonCreator {

    fun createDivider() = """
        {
			"type": "divider"
		}
    """.toJson()

    fun createPlainText(text: String) = """
        {
            "type": "plain_text",
            "text": "$text"
        }
    """.toJson()

    fun createMarkdownText(text: String) = """
        {
            "type": "mrkdwn",
            "text": "$text"
        }
    """.toJson()

    fun createInputBlock(
        blockId: String,
        label: String,
        placeholder: String,
        hint: String,
        optional: Boolean,
        multiline: Boolean = false,
        maxLength: Int = 3000
    ) = """
        {
            "type": "input",
            "block_id": "$blockId",
            "label": $label,
            "element": {
                "type": "plain_text_input",
                "action_id": "action",
                "placeholder": $placeholder,
                "multiline": $multiline,
                "max_length": $maxLength
            },
            "hint": $hint,
            "optional": $optional
        }
    """.toJson()

    fun createSelectBlock(
        blockId: String,
        label: String,
        placeholder: String,
        options: String,
        hint: String = createPlainText(" "),
        optional: Boolean
    ) = """
        {
            "type": "input",
            "block_id": "$blockId",
            "label": $label,
            "element": {
                "type": "static_select",
                "action_id": "action",
                "placeholder": $placeholder,
                "options": $options
            },
            "hint": $hint,
            "optional": $optional
        }
    """.toJson()

    fun createOption(text: String, value: String) = """
        {
            "text": ${createPlainText(text)},
            "value": "$value"
        }
    """.toJson()
}