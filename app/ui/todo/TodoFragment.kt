package com.example.myapplication.ui.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.TodoRepository
import kotlinx.coroutines.launch

class TodoFragment : Fragment() {
    private lateinit var todoRepository: TodoRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 初始化 Repository
        val database = AppDatabase.getDatabase(requireContext())
        todoRepository = TodoRepository(database)

        // 加载待办事项数据
        lifecycleScope.launch {
            todoRepository.getAllTodos().collect {
                // 这里可以更新 UI 显示待办事项列表
            }
        }

        return inflater.inflate(R.layout.fragment_todo, container, false)
    }
}