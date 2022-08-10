package it.airgap.beaconsdkdemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import it.airgap.beaconsdkdemo.dapp.DAppFragment
import it.airgap.beaconsdkdemo.wallet.WalletFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val walletFragment = WalletFragment()
        val dAppFragment = DAppFragment()

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.wallet -> changeFragment(walletFragment)
                R.id.dapp -> changeFragment(dAppFragment)
                else -> { false }
            }
        }

        bottomNavigationView.selectedItemId = R.id.wallet
    }

    private fun changeFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction().apply {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainer, fragment)
        }.commit()

        return true
    }
}
