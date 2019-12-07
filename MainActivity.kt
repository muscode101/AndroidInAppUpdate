class MainActivity : DaggerAppCompatActivity() {

    private lateinit var myAppUpdater: MyAppUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbarMain)
   
        initMyAppUpdater()
    }

    private fun initMyAppUpdater() {
        myAppUpdater = MyAppUpdater(this)
        myAppUpdater.checkUpdates()
    }
   
    override fun onResume() {
        super.onResume()
        myAppUpdater.onResumeAppUpdate()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
                myAppUpdater.updateRequestCode -> {
                    myAppUpdater.checkUpdates()
                }
            }
        }
    }
}
