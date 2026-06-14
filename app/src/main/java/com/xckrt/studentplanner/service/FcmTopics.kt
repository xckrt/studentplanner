package com.xckrt.studentplanner.service

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
object FcmTopics {
    private fun topic(groupId: Int) = "group_$groupId"
    fun switchGroup(newGroupId: Int, oldGroupId: Int?) {
        if (oldGroupId != null && oldGroupId > 0 && oldGroupId != newGroupId) {
            Firebase.messaging.unsubscribeFromTopic(topic(oldGroupId))
                .addOnCompleteListener { t ->
                    Log.d("FCM", "Отписка от ${topic(oldGroupId)}: успех=${t.isSuccessful}")
                }
        }
        if (newGroupId > 0) {
            Firebase.messaging.subscribeToTopic(topic(newGroupId))
                .addOnCompleteListener { t ->
                    Log.d("FCM", "Подписка на ${topic(newGroupId)}: успех=${t.isSuccessful}")
                }
        }
    }
    fun unsubscribe(groupId: Int?) {
        if (groupId != null && groupId > 0) {
            Firebase.messaging.unsubscribeFromTopic(topic(groupId))
                .addOnCompleteListener { t ->
                    Log.d("FCM", "Отписка от ${topic(groupId)}: успех=${t.isSuccessful}")
                }
        }
    }
}
