package re.notifica.go.ui.intro

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import re.notifica.go.ui.intro.children.IntroLocationFragment
import re.notifica.go.ui.intro.children.IntroLoginFragment
import re.notifica.go.ui.intro.children.IntroNotificationsFragment
import re.notifica.go.ui.intro.children.IntroWelcomeFragment

class IntroAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun createFragment(position: Int): Fragment {
        return when (IntroPage.values()[position]) {
            IntroPage.WELCOME -> IntroWelcomeFragment()
            IntroPage.NOTIFICATIONS -> IntroNotificationsFragment()
            IntroPage.LOCATION -> IntroLocationFragment()
            IntroPage.LOGIN -> IntroLoginFragment()
        }
    }

    override fun getItemCount(): Int = IntroPage.values().size
}
