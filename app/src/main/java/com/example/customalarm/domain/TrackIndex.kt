package com.example.customalarm.domain

object TrackIndex {
    /** 将 nextIndex 修正到 [0, trackCount) 区间；曲目为空时回退 0。 */
    fun normalize(nextIndex: Int, trackCount: Int): Int =
        if (trackCount <= 0) 0 else ((nextIndex % trackCount) + trackCount) % trackCount
}
