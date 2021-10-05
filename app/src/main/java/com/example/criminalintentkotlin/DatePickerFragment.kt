package com.example.criminalintentkotlin

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import java.util.*

private const val ARG_DATE = "date"
private const val ARG_REQUEST_CODE = "requestCode"
private const val RESULT_DATE_KEY = "DateKey"

class DatePickerFragment : DialogFragment() {
    interface Callbacks{
        fun onDateSelected(date: Date)
    }

    companion object{
        fun newInstance(date: Date, requestCode: String): DatePickerFragment{
            val args = Bundle().apply {
                putSerializable(ARG_DATE, date)
                putString(ARG_REQUEST_CODE, requestCode)
            }
            return DatePickerFragment().apply {
                arguments = args
            }
        }

        fun getResult(bundle: Bundle) = bundle.getSerializable(RESULT_DATE_KEY) as Date
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val date = arguments?.getSerializable(ARG_DATE) as Date


        val dateListener = DatePickerDialog.OnDateSetListener{ _: DatePicker, year : Int, month: Int, day: Int ->
            val resultDate: Date = GregorianCalendar(year,month,day).time

            val result = Bundle().apply {
                putSerializable(RESULT_DATE_KEY, resultDate)
            }

            //todo
            parentFragmentManager.setFragmentResult("requestKey",result)

        }

        val calendar = Calendar.getInstance()
        calendar.time = date
        val initialYear = calendar.get(Calendar.YEAR)
        val initialMonth = calendar.get(Calendar.MONTH)
        val  initialDay = calendar.get(Calendar.DAY_OF_MONTH)



        return DatePickerDialog(requireContext(),
            dateListener,
            initialYear,
            initialMonth,
            initialDay)
    }
}