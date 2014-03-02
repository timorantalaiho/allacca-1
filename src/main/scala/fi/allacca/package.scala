package fi

import android.view.View
import android.view.View.{OnFocusChangeListener, OnClickListener}
import org.joda.time.LocalDate

/*
 * When your code is not in allacca-package, but you need these definitions, do this import:
 * import fi.allacca._
 */
package object allacca {
  //For logging
  val TAG = "ALLACCA"

  //Intent keys
  val EVENT_ID = "fi.allacca.eventID"
  val EVENT_DATE = "fi.allacca.eventDate"
  val NULL_VALUE = -1L

  val REQUEST_CODE_EDIT_EVENT = 1

  implicit def func2OnClickListener(f: View => Unit) = new OnClickListener() {
    def onClick(evt: View) = f(evt)
  }

  implicit def func2OnFocusChangeListener(f: (View, Boolean) => Unit) = new OnFocusChangeListener {
    def onFocusChange(view: View, focus: Boolean) = f(view, focus)
  }

  implicit def localDateToEpochMillis(localDate: LocalDate): Long = localDate.toDate.getTime

  implicit def func2Runnable(f: => Unit) = new Runnable() { def run() { f }}
}
