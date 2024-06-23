package repository

import model.Configuration

interface ConfigurationRepository {
    fun load(): Configuration
    fun get(): Configuration
    fun reload()
}
