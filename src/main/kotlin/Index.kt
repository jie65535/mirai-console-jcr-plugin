package top.jie65535.jcr

import kotlinx.serialization.Serializable

@Serializable
class Index(
    val type: String,
    val name: String,
    val link: String,
)