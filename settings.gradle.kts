/// settings.gradle.kts (ant-wrapper):
/// =================================
///
/// Access gradle.properties:
///     yes -> "val prop_name = settings.extra['prop.name']"
///     no  -> "val prop_name = String by settings"


/** 1) Configuration for buildscript plugin dependencies */
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}


/** 2) Set plugin name */
rootProject.name = settings.extra["software.name"]!! as String
