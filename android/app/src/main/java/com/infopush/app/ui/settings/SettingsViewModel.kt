package com.infopush.app.ui.settings

import com.infopush.app.domain.ImportMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val jsonText: String = "",
    val message: String = ""
)

class SettingsViewModel(
    private val exportLocalJson: suspend () -> String,
    private val importLocalJson: suspend (String, ImportMode) -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateJsonText(value: String) {
        _uiState.value = _uiState.value.copy(jsonText = value)
    }

    fun updateMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    fun prepareExport(onSuccess: (String) -> Unit = {}) {
        scope.launch {
            runCatching { exportLocalJson() }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(jsonText = it, message = "导出成功")
                    onSuccess(it)
                }
                .onFailure { _uiState.value = _uiState.value.copy(message = "导出失败: ${it.message}") }
        }
    }

    fun importData(mode: ImportMode) {
        importFromJson(_uiState.value.jsonText, mode)
    }

    fun importFromJson(json: String, mode: ImportMode) {
        scope.launch {
            runCatching { importLocalJson(json, mode) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        jsonText = json,
                        message = "导入成功(${mode.name.lowercase()})"
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(message = "导入失败: ${it.message}") }
        }
    }
}
