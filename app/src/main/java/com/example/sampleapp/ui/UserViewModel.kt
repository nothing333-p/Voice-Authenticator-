package com.example.sampleapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sampleapp.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            repository.getAllUsers().collect {
                _users.value = it
            }
        }
    }

    fun addUser(name: String, gender: String, audioPaths: List<String>) {
        viewModelScope.launch {
            val user = User(name = name, gender = gender, audioPaths = audioPaths)
            repository.insertUser(user)
            loadUsers()
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            repository.deleteUser(user)
            loadUsers()
        }
    }
}


