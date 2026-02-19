package com.infopush.app.ui.sources

import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.repo.AddSourceResult
import com.infopush.app.data.repo.AiDiscoveredSource
import com.infopush.app.data.repo.ManualAddSourceResult
import com.infopush.app.data.repo.RefreshResult
import com.infopush.app.ui.common.ListUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SourceDraft(
    val name: String = "",
    val url: String = "",
    val type: String = "rss",
    val tags: String = ""
)

data class AiSearchUiState(
    val keyword: String = "",
    val loading: Boolean = false,
    val results: List<AiDiscoveredSource> = emptyList(),
    val error: String? = null,
    val empty: Boolean = false,
    val addedUrls: Set<String> = emptySet()
)

class SourcesViewModel(
    private val observeSources: () -> Flow<List<SourceEntity>>,
    private val refreshSources: suspend () -> RefreshResult,
    private val addSource: suspend (SourceDraft) -> ManualAddSourceResult,
    private val setSourceEnabled: suspend (sourceId: String, enabled: Boolean) -> Unit,
    private val deleteSource: suspend (sourceId: String) -> Unit,
    private val discoverSources: suspend (keyword: String) -> List<AiDiscoveredSource>,
    private val addAiSourceToLocal: suspend (AiDiscoveredSource) -> AddSourceResult,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow(ListUiState<SourceEntity>(loading = true))
    val uiState: StateFlow<ListUiState<SourceEntity>> = _uiState.asStateFlow()

    private val _aiSearchState = MutableStateFlow(AiSearchUiState())
    val aiSearchState: StateFlow<AiSearchUiState> = _aiSearchState.asStateFlow()

    private val _sourceFilter = MutableStateFlow("")
    val sourceFilter: StateFlow<String> = _sourceFilter.asStateFlow()

    init {
        scope.launch {
            observeSources().collectLatest { sources ->
                val sourceUrls = sources.map { it.url.trim() }.filter { it.isNotBlank() }.toSet()
                _uiState.value = _uiState.value.copy(loading = false, items = sources)
                _aiSearchState.value = _aiSearchState.value.copy(addedUrls = sourceUrls)
            }
        }
    }

    fun updateSourceFilter(filter: String) {
        _sourceFilter.value = filter
    }

    fun filteredSources(): List<SourceEntity> {
        val keyword = _sourceFilter.value.trim().lowercase()
        if (keyword.isBlank()) return _uiState.value.items
        return _uiState.value.items.filter { source ->
            source.name.lowercase().contains(keyword) ||
                source.url.lowercase().contains(keyword) ||
                source.tags.lowercase().contains(keyword)
        }
    }

    fun updateKeyword(keyword: String) {
        _aiSearchState.value = _aiSearchState.value.copy(keyword = keyword, error = null)
    }

    fun searchAiSources() {
        val keyword = _aiSearchState.value.keyword.trim()
        if (keyword.isBlank()) {
            _aiSearchState.value = _aiSearchState.value.copy(error = "请输入关键词")
            return
        }
        scope.launch {
            _aiSearchState.value = _aiSearchState.value.copy(loading = true, error = null, empty = false, results = emptyList())
            runCatching { discoverSources(keyword) }
                .onSuccess { results ->
                    _aiSearchState.value = _aiSearchState.value.copy(
                        loading = false,
                        results = results,
                        empty = results.isEmpty()
                    )
                }
                .onFailure { t ->
                    _aiSearchState.value = _aiSearchState.value.copy(
                        loading = false,
                        error = t.message ?: "搜索失败"
                    )
                }
        }
    }

    fun addDiscoveredSource(candidate: AiDiscoveredSource) {
        val normalizedUrl = candidate.url.trim()
        if (_aiSearchState.value.addedUrls.contains(normalizedUrl)) {
            _aiSearchState.value = _aiSearchState.value.copy(error = "该 URL 已存在于本地信息源")
            return
        }
        scope.launch {
            when (val result = addAiSourceToLocal(candidate)) {
                AddSourceResult.Success -> {
                    _aiSearchState.value = _aiSearchState.value.copy(error = null)
                }

                AddSourceResult.Duplicated -> {
                    _aiSearchState.value = _aiSearchState.value.copy(error = "该 URL 已存在于本地信息源")
                }

                is AddSourceResult.Invalid -> {
                    _aiSearchState.value = _aiSearchState.value.copy(error = result.message)
                }
            }
        }
    }

    fun manualSync() {
        scope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val result = refreshSources()) {
                is RefreshResult.Success -> {
                    _uiState.value = _uiState.value.copy(loading = false, fromMock = result.fromMock)
                }

                is RefreshResult.Error -> {
                    _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun createSource(draft: SourceDraft) {
        if (draft.name.isBlank() || draft.url.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "名称和 URL 不能为空", notice = null)
            return
        }
        scope.launch {
            when (val result = addSource(draft)) {
                ManualAddSourceResult.Success -> {
                    _uiState.value = _uiState.value.copy(error = null, notice = "测试通过并已添加")
                }

                is ManualAddSourceResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message, notice = null)
                }
            }
        }
    }

    fun toggleSource(source: SourceEntity) {
        scope.launch {
            setSourceEnabled(source.id, !source.enabled)
        }
    }

    fun removeSource(sourceId: String) {
        scope.launch {
            deleteSource(sourceId)
        }
    }
}
