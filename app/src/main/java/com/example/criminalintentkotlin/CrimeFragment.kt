package com.example.criminalintentkotlin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import java.io.File
import java.util.*

private const val ARG_CRIME_ID = "crime_id"
private const val TAG = "CrimeFragment"
private const val REQUEST_DATE = "DialogDate"
private const val DATE_FORMAT = "EEEE, MMM dd, yyyy"
private const val REQUEST_CONTACT = 1
private const val REQUEST_PHOTO = 2

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks {

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    private lateinit var titleFiled: EditText
    private lateinit var dateButton: Button
    private lateinit var reportButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var callPoliceCheckBox: CheckBox
    private lateinit var suspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView


    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeDetailViewModel::class.java)
    }

    companion object{
        fun newInstance(crimeId: UUID): CrimeFragment{
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply { arguments = args }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()

        val crimeId = arguments?.getSerializable(ARG_CRIME_ID) as UUID

        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)

        titleFiled = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        callPoliceCheckBox = view.findViewById(R.id.call_police) as CheckBox
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView


        reportButton.setOnClickListener{
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }


        childFragmentManager.setFragmentResultListener("requestKey", this){ _, bundle ->
            var result = DatePickerFragment.getResult(bundle)
            crime.date = result
            updateUI()
        }

        dateButton.setOnClickListener{
            DatePickerFragment
                .newInstance(crime.date, REQUEST_DATE).apply {

                show(this@CrimeFragment.childFragmentManager, REQUEST_DATE)
            }


        }

        solvedCheckBox.apply {
            setOnCheckedChangeListener{ _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        callPoliceCheckBox.apply {
            setOnCheckedChangeListener{ _, requiresPolice ->
                crime.requiresPolice = requiresPolice
            }
        }


        suspectButton.apply{
            val crimeSuspectContract = CrimeSuspectContract()
            val activityLauncher = registerForActivityResult(crimeSuspectContract){ contactUri ->
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                val cursor = contactUri?.let {
                    requireActivity().contentResolver.query(it, queryFields, null, null, null)
                }
                cursor?.use{
                    if (it.count != 0){
                        it.moveToFirst()
                        val suspect = it.getString(0)

                        crime.suspect = suspect
                        crimeDetailViewModel.saveCrime(crime)
                        suspectButton.text = suspect
                    }
                }
            }
            setOnClickListener {
                activityLauncher.launch( REQUEST_CONTACT)
            }


        }

        permissionSetup()

        return view
    }



    override fun onStart() {
        super.onStart()

        var titleWatcher = object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }


        titleFiled.addTextChangedListener(titleWatcher)

        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager
            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(captureImage, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null){
                isEnabled = false
                Log.d(TAG, "onStart: resolvedActivity")
            }


            setOnClickListener{
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                val cameraActivities: List<ResolveInfo> = packageManager.queryIntentActivities(captureImage, PackageManager.MATCH_DEFAULT_ONLY)

                for (cameraActivity in cameraActivities){
                    requireActivity().grantUriPermission(cameraActivity.activityInfo.packageName, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }

                startActivityForResult(captureImage, REQUEST_PHOTO)
            }
        }


    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner,
            androidx.lifecycle.Observer { crime ->
                crime?.let {
                    this.crime = crime
                    photoFile = crimeDetailViewModel.getPhotoFile(crime)
                    photoUri = FileProvider.getUriForFile(requireActivity(), "com.example.criminalintentkotlin.fileprovider", photoFile)
                    updateUI()
                }
            })
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when{
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_PHOTO ->{
                requireActivity().revokeUriPermission(photoUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                updatePhotoView()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    private fun updateUI(){
        Log.d(TAG, "updateUI: $crime")
        
        titleFiled.setText(crime.title)
        dateButton.text = DateFormat.format(DATE_FORMAT, crime.date)
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        callPoliceCheckBox.apply {
            isChecked = crime.requiresPolice
            jumpDrawablesToCurrentState()
        }

        if (crime.suspect.isNotEmpty()){
            suspectButton.text = crime.suspect
        }
        updatePhotoView()
    }

    private fun updatePhotoView(){
        if (photoFile.exists()){
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
        }
        else{
            photoView.setImageDrawable(null)
        }
    }

    private fun getCrimeReport(): String{
        val solvedString = if (crime.isSolved) getString(R.string.crime_report_solved) else getString(R.string.crime_report_unsolved)
        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        var suspect = if (crime.suspect.isBlank()) getString(R.string.crime_report_no_suspect) else getString(R.string.crime_report_suspect, crime.suspect)

        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }


    private class CrimeSuspectContract : ActivityResultContract<Int, Uri?>() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        override fun createIntent(context: Context, input: Int?): Intent {
            return intent
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
            when{
                resultCode != Activity.RESULT_OK -> null
                else -> intent?.data
            }

    }

    private class CrimeCaptureImageContract : ActivityResultContract<Int, Uri?>() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        override fun createIntent(context: Context, input: Int?): Intent {
            return intent
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent?.data
        }

    }


    private fun permissionSetup(){
        val permissionReadContact = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.READ_CONTACTS
        )


        if (permissionReadContact != PackageManager.PERMISSION_GRANTED){
            permissionsResultCallback.launch(Manifest.permission.READ_CONTACTS)
        }
        else{
            Log.d(TAG, "Permission read_contact isGranted")
        }

    }

    private val permissionsResultCallback = registerForActivityResult(
        ActivityResultContracts.RequestPermission()){
        when (it){
            true -> {
                Log.d(TAG, "Permission has been granted by user")
                suspectButton.isEnabled = true
            }
            false ->{
                Toast.makeText(requireContext(), "Read Contact permission denied", Toast.LENGTH_SHORT).show()
                suspectButton.isEnabled = false
            }
        }
    }


}