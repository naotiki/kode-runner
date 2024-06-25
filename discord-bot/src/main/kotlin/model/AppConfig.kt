package model

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.mamoe.yamlkt.Comment

@Serializable
data class AppConfig(
    //127.0.0.1:8080
    @Comment("接続先Runnerのホストとポート")
    val serverHost: String = "localhost:8080",

    val allowed: Allowed = Allowed(emptyList(), emptyList()),

    val env: String = "dev",
    val tokens: Map<String, String> = mapOf("dev" to "<TOKEN>")
)

@Serializable
data class Allowed(
    @Comment("コマンドの実行を許可するユーザーID (ギルドより優先)")
    val users: List<SnowFlakeAsLong>,
    @Comment("コマンドの実行を許可するギルドID")
    val guilds: List<SnowFlakeAsLong>
)

typealias SnowFlakeAsLong = @Serializable(SnowflakeSerializer::class) Snowflake

object SnowflakeSerializer : KSerializer<Snowflake> {
    override val descriptor = PrimitiveSerialDescriptor("Snowflake", PrimitiveKind.LONG)
    override fun deserialize(decoder: Decoder): Snowflake = Snowflake(decoder.decodeLong())
    override fun serialize(encoder: Encoder, value: Snowflake) = encoder.encodeLong(value.value.toLong())
}