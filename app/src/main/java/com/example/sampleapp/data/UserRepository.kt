package com.example.sampleapp.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UserRepository(private val userDao: UserDao) {

    suspend fun insertUser(user: User) = userDao.insertUser(user)

    suspend fun updateUser(user: User) = userDao.updateUser(user)

    suspend fun deleteUser(user: User) = userDao.deleteUser(user)

    fun getAllUsers(): Flow<List<User>> = flow {
        emit(userDao.getAllUsers())
    }

    fun getUserById(id: Int): Flow<User?> = flow {
        emit(userDao.getUserById(id))
    }

    fun getUserByName(name: String): Flow<User?> = flow {
        emit(userDao.getUserByName(name))
    }
}


