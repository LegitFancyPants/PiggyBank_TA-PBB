package com.example.piggybank

sealed class Screen(val route: String) {
    data object Transactions : Screen("transactions")

    data object TransactionDetail : Screen("transaction_detail/{id}") {
        fun createRoute(id: Long): String = "transaction_detail/$id"
    }

    data object TransactionForm : Screen("transaction_form?id={id}") {
        fun createRoute(id: Long? = null): String =
            if (id != null) "transaction_form?id=$id" else "transaction_form"
    }

    data object Goals : Screen("goals")

    data object GoalDetail : Screen("goal_detail/{id}") {
        fun createRoute(id: Long): String = "goal_detail/$id"
    }

    data object GoalForm : Screen("goal_form?id={id}") {
        fun createRoute(id: Long? = null): String =
            if (id != null) "goal_form?id=$id" else "goal_form"
    }

    data object About : Screen("about")

    data object AboutDetail : Screen("about_detail")
}
