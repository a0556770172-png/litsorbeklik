package com.litsorbeklik.app.ui.navigation

object Routes {
    const val REGISTER = "register"
    const val LOGIN = "login"
    const val PROJECTS = "projects"
    const val SPEC = "spec/{projectId}"
    const val SETTINGS = "settings/{projectId}"
    const val BUILD = "build/{projectId}"

    fun spec(projectId: String) = "spec/$projectId"
    fun settings(projectId: String) = "settings/$projectId"
    fun build(projectId: String) = "build/$projectId"
}
