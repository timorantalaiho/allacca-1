package fi.allacca

import android.app._
import android.os.Bundle
import android.widget._
import android.view.{View, ViewGroup}
import android.graphics.{Point, Color}
import android.view.ViewGroup.LayoutParams
import java.text.DateFormatSymbols
import java.util.{Locale, Calendar}
import android.content.Intent
import org.joda.time.{DateTime, LocalDate}
import android.view.animation.{Animation, AlphaAnimation}
import android.view.animation.Animation.AnimationListener
import fi.allacca.Logger._

class AllaccaMain extends Activity with TypedViewHolder {
  private lazy val dimensions = new ScreenParameters(getResources.getDisplayMetrics)
  private lazy val weeksAdapter = new WeeksAdapter(this, dimensions, onWeeksListDayClick, onWeeksListDayLongClick)
  private lazy val weeksList = new WeeksView(this, weeksAdapter, shownMonthsView)

  private lazy val cornerView = new TextView(this)
  private lazy val addEventButton = new Button(this)
  private lazy val shownMonthsView = new ShownMonthsView(this, dimensions)
  private lazy val agendaView = new AgendaView(this, cornerView)
  private lazy val flashingPanel = createFlashingPanel
  private lazy val fade = new AlphaAnimation(1, 0)
  private val idGenerator = new IdGenerator

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    Logger.setDebugFlag(this)
    val mainLayout = createMainLayout

    createShownMonthsView
    mainLayout.addView(shownMonthsView)
    createTopLeftCornerView
    mainLayout.addView(cornerView)

    val titles = createDayColumnTitles()
    titles.foreach { mainLayout.addView }

    createAddEventButton(mainLayout)
    addGotoNowButton(mainLayout, addEventButton.getId)

    val weeksList = createWeeksList()
    mainLayout.addView(weeksList)

    createAgenda(mainLayout)
    mainLayout.addView(flashingPanel)

    setContentView(mainLayout)
  }

  def createMainLayout: RelativeLayout = {
    val mainLayout = new RelativeLayout(this)
    val mainLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    mainLayout.setLayoutParams(mainLayoutParams)
    mainLayout
  }

  private def createTopLeftCornerView: TextView = {
    val params = new RelativeLayout.LayoutParams(dimensions.monthLetterWidth + dimensions.weekNumberWidth, dimensions.weekViewHeaderHeight)
    params.addRule(RelativeLayout.BELOW, shownMonthsView.getId)
    params.addRule(RelativeLayout.ALIGN_LEFT)
    cornerView.setLayoutParams(params)
    cornerView.setId(idGenerator.nextId)
    cornerView.setText("Hello")
    cornerView
  }

  private def createShownMonthsView: ShownMonthsView = {
    val params = new RelativeLayout.LayoutParams(dimensions.weekListWidth, dimensions.weekViewHeaderHeight)
    params.setMargins(dimensions.monthLetterWidth + dimensions.weekNumberWidth, 0, 0, 0)
    shownMonthsView.setLayoutParams(params)
    shownMonthsView.setId(idGenerator.nextId)
    shownMonthsView.setTextSize(dimensions.overviewContentTextSize)
    shownMonthsView
  }

  def createWeeksList(): View = {
    weeksList.setId(idGenerator.nextId)
    weeksList.start()
    val weeksListParams = new RelativeLayout.LayoutParams(dimensions.weekListWidth, LayoutParams.WRAP_CONTENT)
    weeksListParams.setMargins(0, 0, dimensions.weekListRightMargin, 0)
    weeksListParams.addRule(RelativeLayout.BELOW, cornerView.getId)
    weeksListParams.addRule(RelativeLayout.ABOVE, addEventButton.getId)
    weeksListParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
    weeksList.setLayoutParams(weeksListParams)
    weeksList.setDividerHeight(0)
    goToDayOnWeeksList(new DateTime(initialFocusDate.toDate.getTime))
    weeksList
  }

  private def screenSize: Point = {
    val display = getWindowManager.getDefaultDisplay
    val size = new Point()
    display.getSize(size)
    size
  }

  def createAgenda(mainLayout: RelativeLayout) {
    val scrollParams = new RelativeLayout.LayoutParams(screenSize.x - weeksList.getWidth, LayoutParams.MATCH_PARENT)
    scrollParams.addRule(RelativeLayout.RIGHT_OF, weeksList.getId)
    agendaView.setLayoutParams(scrollParams)
    agendaView.setId(idGenerator.nextId)
    mainLayout.addView(agendaView)
    agendaView.start(initialFocusDate)
  }

  private def initialFocusDate: LocalDate = {
    val focusDateMillis = getIntent.getLongExtra(FOCUS_DATE_EPOCH_MILLIS, NULL_VALUE)
    if (focusDateMillis == NULL_VALUE) {
      new LocalDate
    } else {
      new LocalDate(focusDateMillis)
    }
  }

  private def createAddEventButton(layout: ViewGroup): Button = {
    addEventButton.setId(idGenerator.nextId)
    val params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
    addEventButton.setLayoutParams(params)
    addEventButton.setText("+")
    addEventButton.setTextColor(Color.WHITE)
    addEventButton.setOnClickListener(createNewEvent _)
    layout.addView(addEventButton)
    addEventButton
  }

  def createNewEvent (view: View) {
    debug("+ createNewEvent")
    val intent = new Intent(this, classOf[EditEventActivity])

    //This will create the new event after ten days:
    //intent.putExtra(EVENT_DATE, new DateTime().plusDays(10).toDate.getTime)
    intent.putExtra(FOCUS_DATE_EPOCH_MILLIS, agendaView.focusDay.toDate.getTime)
    startActivityForResult(intent, REQUEST_CODE_EDIT_EVENT)
  }

  private def onWeeksListDayClick (day: DateTime) {
    agendaView.focusOn(day.withTimeAtStartOfDay.toLocalDate)
  }

  private def onWeeksListDayLongClick(day: DateTime) = {
    agendaView.focusOn(new LocalDate(day.getMillis))
    debug("onWeeksListDayLongClick")
    val intent = new Intent(this, classOf[EditEventActivity])
    intent.putExtra(EVENT_DATE, day.getMillis + new DateTime().getMillisOfDay)
    intent.putExtra(FOCUS_DATE_EPOCH_MILLIS, day.getMillis)
    startActivityForResult(intent, REQUEST_CODE_EDIT_EVENT)
    true
  }

  private def addGotoNowButton(layout: ViewGroup, leftSideId: Int) {
    val b = new Button(this)
    b.setId(idGenerator.nextId)
    val params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
    params.addRule(RelativeLayout.RIGHT_OF, leftSideId)
    b.setLayoutParams(params)
    b.setText("Now")
    b.setTextColor(Color.WHITE)
    b.setOnClickListener(gotoNow _)
    layout.addView(b)
  }

  private def createFlashingPanel: View = {
    val panel = new FrameLayout(this)
    panel.setId(idGenerator.nextId)
    val params = new RelativeLayout.LayoutParams(getResources.getDisplayMetrics.widthPixels, getResources.getDisplayMetrics.heightPixels)
    panel.setLayoutParams(params)
    panel.setBackgroundColor(dimensions.pavlova)
    panel.setVisibility(View.GONE)

    fade.setDuration(300)
    fade.setAnimationListener(new AnimationListener() {
      def onAnimationEnd(animation: Animation) {
        panel.setVisibility(View.GONE)
      }

      def onAnimationStart(animation: Animation) {
        panel.setVisibility(View.VISIBLE)
      }

      def onAnimationRepeat(animation: Animation) {}
    })
    panel
  }

  def gotoNow(view: View) {
    val now = new DateTime
    goToDayOnWeeksList(now)
    agendaView.focusOn(now.withTimeAtStartOfDay.toLocalDate)
    flashingPanel.startAnimation(fade)
  }

  def goToDayOnWeeksList(day: DateTime) {
    val index = weeksAdapter.rollToDate(day)
    weeksAdapter.onDayClick(day)
    weeksList.setSelection(index)
  }

  private def createDayColumnTitles(): Seq[View] = {
    val shortWeekDays = new DateFormatSymbols(Locale.ENGLISH).getShortWeekdays
    val weekDayInitials = List(
      shortWeekDays(Calendar.MONDAY),
      shortWeekDays(Calendar.TUESDAY),
      shortWeekDays(Calendar.WEDNESDAY),
      shortWeekDays(Calendar.THURSDAY),
      shortWeekDays(Calendar.FRIDAY),
      shortWeekDays(Calendar.SATURDAY),
      shortWeekDays(Calendar.SUNDAY)
    ).map { _.charAt(0).toString }
    weekDayInitials.map { c =>
      val view = new TextView(this)
      view.setId(idGenerator.nextId)
      val layoutParams = new RelativeLayout.LayoutParams(dimensions.dayColumnWidth, dimensions.weekViewHeaderHeight)
      layoutParams.addRule(RelativeLayout.RIGHT_OF, view.getId - 1)
      layoutParams.addRule(RelativeLayout.BELOW, shownMonthsView.getId)
      view.setLayoutParams(layoutParams)
      view.setTextSize(dimensions.overviewHeaderTextSize)
      view.setText(c)
      view
    }
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    info(s"onActivityResult requestCode $requestCode resultCode $resultCode")
    if (requestCode == REQUEST_CODE_EDIT_EVENT && resultCode == Activity.RESULT_OK) refresh(data)
  }

  private def refresh(intentFromOtherActivity: Intent) {
    info("Refreshing main view")
    val intent = getIntent
    intent.putExtra(FOCUS_DATE_EPOCH_MILLIS, intentFromOtherActivity.getLongExtra(FOCUS_DATE_EPOCH_MILLIS, NULL_VALUE))
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
    finish()
    startActivity(intent)
  }
}
