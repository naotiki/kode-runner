package data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.withEdgecases
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.collection


class DataSizeTests : StringSpec({
    val edgeCases=listOf(0, 1, 4, 5, 8, 1023, 1024, 1025)
    "fromString()" {
        checkAll(
            iterations = 10,
            Arb.int(0).withEdgecases(edgeCases),
            Arb.enum<DataSize.ByteUnit>()
        ) { v, unit ->
            DataSize.fromString("$v$unit") shouldBeEqual DataSize(v * unit.toLong())
        }
    }
    "toString()" {
        checkAll(
            Exhaustive.collection(DataSize.ByteUnit.entries.map { u ->
                edgeCases.map { it * u.toLong() }
            }.flatten()),
        ) { v ->
            val data = DataSize(v)
            DataSize.fromString(data.toString()).bytes shouldBeEqual v
        }
    }
})
