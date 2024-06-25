package model


class StackString(@Volatile private var string: String, private val size: Int) {

    fun push(value: String) = synchronized(this) {
        string += value
        val over = string.length - size
        if (over > 0) {
            string = string.drop(over)
        }
    }

    fun popAll(): String = synchronized(this) {
        return string.apply {
            string = ""
        }
    }
}

