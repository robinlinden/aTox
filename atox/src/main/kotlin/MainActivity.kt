package ltd.evilcorp.atox

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.android.AndroidInjection
import ltd.evilcorp.atox.di.ViewModelFactory
import javax.inject.Inject
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.add_contact_fragment.*

class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var vmFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            val raw = result.contents

            //TODO: why does it crash? toxId.setText(raw.removePrefix("tox:"))
            toxId.setText(raw)
        }
        else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
