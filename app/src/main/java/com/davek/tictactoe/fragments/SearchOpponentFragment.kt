package com.davek.tictactoe.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.davek.tictactoe.R
import com.davek.tictactoe.activities.RequestDialogActivity
import com.davek.tictactoe.activities.RequestDialogActivity.Companion.IS_SENDING_REQUEST_EXTRA_ID
import com.davek.tictactoe.activities.RequestDialogActivity.Companion.RECIPIENT_NICKNAME_EXTRA_ID
import com.davek.tictactoe.databinding.FragmentSearchOpponentBinding
import com.davek.tictactoe.databinding.ListItemUserBinding
import com.davek.tictactoe.models.PlayerItem
import com.davek.tictactoe.models.User
import com.davek.tictactoe.viewmodels.SearchOpponentViewModel
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.LoadingState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SearchOpponentFragment : Fragment() {

    private val mTag = this::class.java.simpleName

    private lateinit var binding: FragmentSearchOpponentBinding
    private lateinit var viewModel: SearchOpponentViewModel
    private lateinit var pagingAdapter: FirestorePagingAdapter<PlayerItem, UserViewHolder>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_search_opponent, container, false
        )
        val vm: SearchOpponentViewModel by viewModels()
        viewModel = vm
        observeViewModel(viewModel)
        binding.searchVM = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        Log.i(mTag, "onStart() called")
        viewModel.addToWaitingListIfInitialised()
    }

    override fun onStop() {
        super.onStop()
        viewModel.removeFromWaitingList()
        Log.i(mTag, "onStop() called")
    }

    private fun observeViewModel(vm: SearchOpponentViewModel) {
        vm.currentAndOpponentNicknames.observe(viewLifecycleOwner, {
            setupRecyclerView(vm, it)
        })

        vm.currentGameId.observe(viewLifecycleOwner, {
            if (it != "") {
                findNavController().navigate(
                    MainOnlineFragmentDirections.actionMainOnlineFragmentToBoardFragment(it, null)
                )
            }
        })

        vm.showAddNicknameDialog.observe(viewLifecycleOwner, {
            showAddNicknameDialog(it)
        })

        vm.showSendingRequestDialog.observe(viewLifecycleOwner, { recipient ->
            val intent = Intent(requireContext(), RequestDialogActivity::class.java).apply {
                putExtra(IS_SENDING_REQUEST_EXTRA_ID, true)
                putExtra(RECIPIENT_NICKNAME_EXTRA_ID, recipient.player.nickname)
            }
            startActivity(intent)
        })

        vm.refreshList.observe(viewLifecycleOwner, {
            if (this::pagingAdapter.isInitialized) {
                pagingAdapter.refresh()
            }
        })
    }

    private fun showAddNicknameDialog(alreadyTakenNickname: String?) {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_add_nickname_title))
        if (alreadyTakenNickname == null) {
            builder.setView(R.layout.dialog_add_nickname)
        } else {
            val view: View =
                requireActivity().layoutInflater.inflate(R.layout.dialog_add_nickname, null)
            view.findViewById<TextInputLayout>(R.id.text_input_layout).error =
                String.format(
                    resources.getString(R.string.dialog_add_nickname_already_taken_error),
                    alreadyTakenNickname
                )
            builder.setView(view)
        }
        builder
            .setPositiveButton(getString(R.string.dialog_add_nickname_positive_button)) { dialog, _ ->
                val input = (dialog as AlertDialog).findViewById<TextView>(android.R.id.text1)
                viewModel.addNewNickname(input?.text.toString())
            }
            .setNegativeButton(getString(R.string.dialog_add_nickname_negative_button)) { _, _ ->
                findNavController().navigateUp()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun setupRecyclerView(
        vm: SearchOpponentViewModel,
        currentAndOpponentNicknames: Pair<String, String>
    ) {
        val currentUserNickname = currentAndOpponentNicknames.first
        val nicknameToSearch = currentAndOpponentNicknames.second
        val baseQuery: Query = if (nicknameToSearch != "") {
            FirebaseFirestore.getInstance()
                .collection("waitingList")
                .whereEqualTo("nickname", nicknameToSearch)
                .whereNotEqualTo("nickname", currentUserNickname)
        } else {
            FirebaseFirestore.getInstance()
                .collection("waitingList")
                .whereNotEqualTo("nickname", currentUserNickname)
        }

        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPrefetchDistance(10)
            .setPageSize(20)
            .build()
        val options: FirestorePagingOptions<PlayerItem> =
            FirestorePagingOptions.Builder<PlayerItem>()
                .setLifecycleOwner(this)
                .setQuery(
                    baseQuery,
                    config
                ) { snapshot ->
                    val user = snapshot.toObject(User::class.java) ?: User()
                    PlayerItem(user, snapshot.id)
                }.build()
        pagingAdapter = object : FirestorePagingAdapter<PlayerItem, UserViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
                return UserViewHolder.from(parent)
            }

            override fun onBindViewHolder(
                holder: UserViewHolder,
                position: Int,
                model: PlayerItem
            ) {
                holder.bind(vm, model)
            }

            override fun onLoadingStateChanged(state: LoadingState) {
                when (state) {
                    LoadingState.LOADING_INITIAL,
                    LoadingState.LOADING_MORE -> binding.swipeRefreshLayout.isRefreshing = true
                    LoadingState.LOADED -> binding.swipeRefreshLayout.isRefreshing = false
                    LoadingState.FINISHED -> binding.swipeRefreshLayout.isRefreshing = false
                    LoadingState.ERROR -> {
                        Log.e(mTag, "Error: onLoadingStateChanged")
                        retry()
                    }
                }
            }

            override fun onError(e: Exception) {
                binding.swipeRefreshLayout.isRefreshing = false
                Log.e(mTag, e.message, e)
            }
        }
        binding.userList.adapter = pagingAdapter
        binding.userList.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.swipeRefreshLayout.setOnRefreshListener { pagingAdapter.refresh() }
    }

    class UserViewHolder private constructor(
        private val binding: ListItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(viewModel: SearchOpponentViewModel, user: PlayerItem) {
            binding.searchVM = viewModel
            binding.playerItem = user
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): UserViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemUserBinding.inflate(layoutInflater, parent, false)

                return UserViewHolder(binding)
            }
        }
    }
}