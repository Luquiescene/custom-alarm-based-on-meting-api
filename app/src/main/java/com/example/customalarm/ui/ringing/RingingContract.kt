package com.example.customalarm.ui.ringing

/** 响铃链路（Receiver/Activity/调度器）共享的 Intent 契约。 */
object RingingContract {
    const val ACTION_RING = "com.example.customalarm.action.RING"
    const val ACTION_TEST = "com.example.customalarm.action.TEST_RING"

    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_SCHEDULED_AT = "scheduled_at"
    const val EXTRA_ACTUAL_TRIGGER_AT = "actual_trigger_at"
    const val EXTRA_IS_TEST = "is_test"
    const val EXTRA_ACTION = "action"

    const val ACTION_STOP = "stop"
    const val ACTION_SNOOZE = "snooze"
}
