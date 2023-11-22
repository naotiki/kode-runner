package unit.data

import data.DataSize
import data.DataSize.ByteUnit.*
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.equals.shouldBeEqual
import kotlin.test.Test
import kotlin.test.assertEquals



class DataSizeTests : FreeSpec({
    "文字列変換" - {
        "toString()" - {
            "1025GB" - {
                val data= DataSize(1025L*1024*1024*1024)
                data.toString() shouldBeEqual "1025GB"
            }
            "4GB" - {
                val data= DataSize(4L*1024*1024*1024)
                data.toString() shouldBeEqual "4GB"
            }
            "1GB" - {
                val data= DataSize(1024*1024*1024)
                data.toString() shouldBeEqual "1GB"
            }
            "1025KB" - {
                val data= DataSize(1024*1024+1*1024)
                data.toString() shouldBeEqual "1025KB"
            }

            "0B" - {
                val data= DataSize(0)
                data.toString() shouldBeEqual "0B"
            }
        }
        "fromString()" - {
            "1025GB" - {
                DataSize.fromString("1025GB") shouldBeEqual DataSize(1025L*1024*1024*1024)
            }
            "4GB" - {
                DataSize.fromString("4GB") shouldBeEqual DataSize(4L*1024*1024*1024)
            }
            "1GB" - {
                DataSize.fromString("1GB") shouldBeEqual DataSize(1024*1024*1024)
            }
            "1025KB" - {
                DataSize.fromString("1025KB") shouldBeEqual DataSize(1024*1024+1*1024)
            }

            "0GB" - {
                DataSize.fromString("0GB") shouldBeEqual DataSize(0)
            }
            "0B" - {
                DataSize.fromString("0B") shouldBeEqual DataSize(0)
            }
        }
    }
})
