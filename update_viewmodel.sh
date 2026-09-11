sed -i '/fun fetchData() {/i \
    fun refreshData() {\n        viewModelScope.launch {\n            _isRefreshing.value = true\n            fetchData()\n            _isRefreshing.value = false\n        }\n    }\n' app/src/main/java/com/example/ui/home/HomeViewModel.kt
