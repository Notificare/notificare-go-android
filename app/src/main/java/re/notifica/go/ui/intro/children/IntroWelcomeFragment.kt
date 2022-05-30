package re.notifica.go.ui.intro.children

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import re.notifica.go.databinding.FragmentIntroWelcomeBinding
import re.notifica.go.ui.intro.IntroPage
import re.notifica.go.ui.intro.IntroViewModel

@AndroidEntryPoint
class IntroWelcomeFragment : Fragment() {
    private val viewModel: IntroViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private lateinit var binding: FragmentIntroWelcomeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentIntroWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.continueButton.setOnClickListener {
            viewModel.moveTo(IntroPage.NOTIFICATIONS)
        }
    }
}
