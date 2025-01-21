/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.backup.v2

import org.thoughtcrime.securesms.database.CallTable

/**
 * These represent situations where we will skip exporting a data frame due to the data being invalid.
 */
object ExportSkips {
  fun emptyChatItem(sentTimestamp: Long): String {
    return log(sentTimestamp, "Completely empty ChatItem (no inner item is set).")
  }

  fun emptyStandardMessage(sentTimestamp: Long): String {
    return log(sentTimestamp, "Completely empty StandardMessage (no body or attachments).")
  }

  fun invalidLongTextChatItem(sentTimestamp: Long): String {
    return log(sentTimestamp, "ChatItem with a long-text attachment had no body.")
  }

  fun messageExpiresTooSoon(sentTimestamp: Long): String {
    return log(sentTimestamp, "Message expires too soon. Must skip.")
  }

  fun individualCallStateNotMappable(sentTimestamp: Long, event: CallTable.Event): String {
    return log(sentTimestamp, "Unable to map group only status to 1:1 call state. Event: ${event.name}")
  }

  fun failedToParseSharedContact(sentTimestamp: Long): String {
    return log(sentTimestamp, "Failed to parse shared contacts.")
  }

  fun failedToParseGiftBadge(sentTimestamp: Long): String {
    return log(sentTimestamp, "Failed to parse GiftBadge.")
  }

  fun failedToParseGroupUpdate(sentTimestamp: Long): String {
    return log(sentTimestamp, "Failed to parse GroupUpdate.")
  }

  fun groupUpdateHasNoUpdates(sentTimestamp: Long): String {
    return log(sentTimestamp, "Group update record is parseable, but has no updates.")
  }

  fun directStoryReplyHasNoBody(sentTimestamp: Long): String {
    return log(sentTimestamp, "Direct story reply has no body.")
  }

  private fun log(sentTimestamp: Long, message: String): String {
    return "[SKIP][$sentTimestamp] $message"
  }
}

/**
 * These represent situations where we encounter some weird data, but are still able to export the frame. We may have needed to "massage" the data to get
 * it to fit the spec.
 */
object ExportOddities {

  fun revisionsOnUnexpectedMessageType(sentTimestamp: Long): String {
    return log(sentTimestamp, "Attempted to set revisions on message that doesn't support it. Ignoring revisions.")
  }

  fun mismatchedRevisionHistory(sentTimestamp: Long): String {
    return log(sentTimestamp, "Revisions for this message contained items of a different type than the parent item. Ignoring mismatched revisions.")
  }

  fun outgoingMessageWasSentButTimerNotStarted(sentTimestamp: Long): String {
    return log(sentTimestamp, "Outgoing expiring message was sent, but the timer wasn't started. Setting expireStartDate to dateReceived.")
  }

  fun incomingMessageWasReadButTimerNotStarted(sentTimestamp: Long): String {
    return log(sentTimestamp, "Incoming expiring message was read, but the timer wasn't started. Setting expireStartDate to dateReceived.")
  }

  fun failedToParseBodyRangeList(sentTimestamp: Long): String {
    return log(sentTimestamp, "Unable to parse BodyRangeList. Ignoring it.")
  }

  fun failedToParseLinkPreview(sentTimestamp: Long): String {
    return log(sentTimestamp, "Failed to parse link preview. Ignoring it.")
  }

  private fun log(sentTimestamp: Long, message: String): String {
    return "[ODDITY][$sentTimestamp] $message"
  }
}
