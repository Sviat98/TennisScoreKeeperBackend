package com.bashkevich.tennisscorekeeperbackend.model.auth

import org.jetbrains.exposed.v1.core.Table

object UserAuthTable: Table("user_auth"){
    val userId = reference("user_id", UserTable)
    val login = varchar("login", 50)
    val password = binary("password")
    val isAdmin = bool("is_admin")

    override val primaryKey = PrimaryKey(userId)
}
