package routes.interactions.requests

data class InteractionRequestBody(
    val user: User,
    val triggerId: String,
    val view: View
) {
    data class User(
        val id: String
    )

    data class View(
        val callbackId: String,
        val state: State
    ) {
        data class State(
            val values: Values
        ) {
            data class Values(
                val subject: TextValue?,
                val path: TextValue,
                val situation: TextValue,
                val expectedResult: TextValue?,
                val version: SelectValue,
                val server: SelectValue?,
                val etcEnvironment: TextValue?,
                val priority: SelectValue,
                val errorType: SelectValue?,
                val reproducing: SelectValue?,
                val track: SelectValue,
                val developer: SelectValue,
                val channel: SelectValue
            ) {
                data class TextValue(
                    val action: TextAction
                ) {
                    data class TextAction(
                        val value: String?
                    )
                }

                data class SelectValue(
                    val action: SelectAction
                ) {
                    data class SelectAction(
                        val selectedOption: SelectedOption?
                    ) {
                        data class SelectedOption(
                            val value: String
                        )
                    }
                }
            }
        }
    }
}