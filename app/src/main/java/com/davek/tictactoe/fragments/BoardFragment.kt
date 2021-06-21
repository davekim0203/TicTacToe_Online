package com.davek.tictactoe.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.davek.tictactoe.R
import com.davek.tictactoe.activities.GameEndDialogActivity
import com.davek.tictactoe.activities.GameEndDialogActivity.Companion.GAME_END_DIALOG_REQUEST_CODE
import com.davek.tictactoe.activities.GameEndDialogActivity.Companion.GAME_RESULT_EXTRA_ID
import com.davek.tictactoe.activities.GameEndDialogActivity.Companion.NEED_TO_SAVE_EXTRA_ID
import com.davek.tictactoe.activities.GameEndDialogActivity.Companion.NEED_TO_SEND_REPLY_REQUEST_EXTRA_ID
import com.davek.tictactoe.adapters.CellAdapter
import com.davek.tictactoe.databinding.FragmentBoardBinding
import com.davek.tictactoe.viewmodels.BoardViewModel
import com.davek.tictactoe.viewmodels.GameResult
import com.davek.tictactoe.viewmodels.GameSize
import com.davek.tictactoe.viewmodels.OfflineGameResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BoardFragment : Fragment() {

    private val mTag = this::class.java.simpleName

    private val viewModel: BoardViewModel by viewModels()
    private lateinit var binding: FragmentBoardBinding
    private lateinit var cellAdapter: CellAdapter
    private val args: BoardFragmentArgs by navArgs()

    private val gameEndResultHandler = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        result?.let {
            if (it.resultCode == GAME_END_DIALOG_REQUEST_CODE) {
                val intent = it.data
                val needToSave = intent?.getBooleanExtra(NEED_TO_SAVE_EXTRA_ID, false)
                val wantReplay = intent?.getBooleanExtra(NEED_TO_SEND_REPLY_REQUEST_EXTRA_ID, false)
                viewModel.handleGameEndDialogResponse(needToSave, wantReplay)
            } else {
                viewModel.handleGameEndDialogResponse(wantSave = false, wantReplay = false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_board, container, false
        )
        observeViewModel()
        binding.boardVM = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel.initGame(args.gameId, args.savedGame, false)
        setBackPressCallback()

        return binding.root
    }

    private fun observeViewModel() {
        viewModel.gameSize.observe(viewLifecycleOwner, {
            when (it) {
                GameSize.THREE_BY_THREE -> {
                    if (binding.toggleGameSize.checkedButtonId != binding.gameSizeThree.id) {
                        binding.toggleGameSize.check(binding.gameSizeThree.id)
                    }
                }
                GameSize.TEN_BY_TEN -> {
                    if (binding.toggleGameSize.checkedButtonId != binding.gameSizeTen.id) {
                        binding.toggleGameSize.check(binding.gameSizeTen.id)
                    }
                }
            }
            setupRecyclerView(it.boardSpan)
        })

        viewModel.onlineGameResult.observe(viewLifecycleOwner, {
            it?.let { result ->
                if (result != GameResult.IN_GAME) {
                    val intent = Intent(requireContext(), GameEndDialogActivity::class.java).apply {
                        putExtra(GAME_RESULT_EXTRA_ID, result)
                    }
                    gameEndResultHandler.launch(intent)
                }
            }
        })

        viewModel.showQuitConfirmDialog.observe(viewLifecycleOwner, {
            showQuitConfirmDialog()
        })

        viewModel.offlineGameResult.observe(viewLifecycleOwner, {
            showOfflineGameEndDialog(it)
        })

        viewModel.navigateUp.observe(viewLifecycleOwner, {
            findNavController().navigateUp()
        })
    }

    private fun setupRecyclerView(spanSize: Int) {
        cellAdapter = CellAdapter(viewModel)
        binding.rvGameBoard.adapter = cellAdapter
        binding.rvGameBoard.layoutManager =
            GridLayoutManager(requireContext(), spanSize, GridLayoutManager.VERTICAL, false)
    }

    private fun setBackPressCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.d(mTag, "Fragment system back pressed")
                    if (args.gameId == null) {
                        if (isEnabled) {
                            isEnabled = false
                            requireActivity().onBackPressed()
                        }
                    } else {
                        showQuitConfirmDialog()
                    }
                }
            }
        )
    }

    private fun showQuitConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_confirm_quit_title))
            .setMessage(getString(R.string.dialog_confirm_quit_body))
            .setPositiveButton(getString(R.string.dialog_confirm_quit_positive_button)) { dialog, _ ->
                viewModel.quitGame()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.dialog_confirm_quit_negative_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showOfflineGameEndDialog(result: OfflineGameResult) {
        val message = when (result) {
            OfflineGameResult.IN_GAME -> return
            OfflineGameResult.TIE -> getString(R.string.dialog_offline_game_end_body_tie)
            OfflineGameResult.WIN_O -> String.format(
                getString(R.string.dialog_offline_game_end_body),
                "O"
            )
            OfflineGameResult.WIN_X -> String.format(
                getString(R.string.dialog_offline_game_end_body),
                "X"
            )
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_offline_game_end_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_offline_game_end_positive_button)) { dialog, _ ->
                viewModel.initGame(null, null, true)
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
            .show()
    }
}