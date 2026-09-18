package com.bashkevich.tennisscorekeeperbackend.model.auth

data class UserAuthEntity(
    val userId: Int,
    val login: String,
    val hashedPassword: ByteArray,
    val isAdmin: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserAuthEntity

        if (userId != other.userId) return false
        if (isAdmin != other.isAdmin) return false
        if (login != other.login) return false
        if (!hashedPassword.contentEquals(other.hashedPassword)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId
        result = 31 * result + isAdmin.hashCode()
        result = 31 * result + login.hashCode()
        result = 31 * result + hashedPassword.contentHashCode()
        return result
    }

}
