package com.softmotions.runner

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
enum class Platform {
    Linux,
    Windows,
    OS_X,
    Solaris,
    FreeBSD;

    val isUnixLike: Boolean
        get() = this != Windows

    companion object {
        val CURRENT: Platform = {
            val os = System.getProperty("os.name")
            when {
                os == "Linux" -> Linux
                os.startsWith("Windows") -> Windows
                os == "Mac OS X" -> OS_X
                os.contains("SunOS") -> Solaris
                os == "FreeBSD" -> FreeBSD
                else -> {
                    throw IllegalArgumentException("Could not detect Platform: os.name: $os")
                }
            }
        }()
    }
}