package com.bashkevich.tennisscorekeeperbackend.model.auth

data class PlayerAuthEntity(
    val id: Int,
    val login: String,
    val hashedPassword: ByteArray,
    val isAdmin: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerAuthEntity

        if (id != other.id) return false
        if (isAdmin != other.isAdmin) return false
        if (login != other.login) return false
        if (!hashedPassword.contentEquals(other.hashedPassword)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + isAdmin.hashCode()
        result = 31 * result + login.hashCode()
        result = 31 * result + hashedPassword.contentHashCode()
        return result
    }

}
