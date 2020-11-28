package org.koitharu.kotatsu.details.ui

import android.os.Bundle
import android.text.Spanned
import android.view.View
import androidx.core.net.toUri
import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.fragment_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.favourites.ui.categories.select.FavouriteCategoriesDialog
import org.koitharu.kotatsu.reader.ui.ReaderActivity
import org.koitharu.kotatsu.search.ui.MangaSearchSheet
import org.koitharu.kotatsu.utils.FileSizeUtils
import org.koitharu.kotatsu.utils.ext.*
import kotlin.math.roundToInt

class DetailsFragment : BaseFragment(R.layout.fragment_details), View.OnClickListener,
	View.OnLongClickListener {

	private val viewModel by sharedViewModel<DetailsViewModel>()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewModel.manga.observe(viewLifecycleOwner, ::onMangaUpdated)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
		viewModel.favouriteCategories.observe(viewLifecycleOwner, ::onFavouriteChanged)
		viewModel.readingHistory.observe(viewLifecycleOwner, ::onHistoryChanged)
	}

	private fun onMangaUpdated(manga: Manga) {
		imageView_cover.newImageRequest(manga.largeCoverUrl ?: manga.coverUrl)
			.fallback(R.drawable.ic_placeholder)
			.crossfade(true)
			.lifecycle(viewLifecycleOwner)
			.enqueueWith(coil)
		textView_title.text = manga.title
		textView_subtitle.textAndVisible = manga.altTitle
		textView_description.text = manga.description?.parseAsHtml()?.takeUnless(Spanned::isBlank)
			?: getString(R.string.no_description)
		if (manga.rating == Manga.NO_RATING) {
			ratingBar.isVisible = false
		} else {
			ratingBar.progress = (ratingBar.max * manga.rating).roundToInt()
			ratingBar.isVisible = true
		}
		chips_tags.removeAllViews()
		manga.author?.let { a ->
			chips_tags.addChips(listOf(a)) {
				create(
					text = it,
					iconRes = R.drawable.ic_chip_user,
					tag = it,
					onClickListener = this@DetailsFragment
				)
			}
		}
		chips_tags.addChips(manga.tags) {
			create(
				text = it.title,
				iconRes = R.drawable.ic_chip_tag,
				tag = it,
				onClickListener = this@DetailsFragment
			)
		}
		manga.url.toUri().toFileOrNull()?.let { f ->
			viewLifecycleScope.launch {
				val size = withContext(Dispatchers.IO) {
					f.length()
				}
				chips_tags.addChips(listOf(f)) {
					create(
						text = FileSizeUtils.formatBytes(context, size),
						iconRes = R.drawable.ic_chip_storage,
						tag = it,
						onClickListener = this@DetailsFragment
					)
				}
			}
		}
		imageView_favourite.setOnClickListener(this)
		button_read.setOnClickListener(this)
		button_read.setOnLongClickListener(this)
		button_read.isEnabled = !manga.chapters.isNullOrEmpty()
	}

	private fun onHistoryChanged(history: MangaHistory?) {
		with(button_read) {
			if (history == null) {
				setText(R.string.read)
				setIconResource(R.drawable.ic_read)
			} else {
				setText(R.string._continue)
				setIconResource(R.drawable.ic_play)
			}
		}
	}

	private fun onFavouriteChanged(isFavourite: Boolean) {
		imageView_favourite.setImageResource(
			if (isFavourite) {
				R.drawable.ic_heart
			} else {
				R.drawable.ic_heart_outline
			}
		)
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		progressBar.isVisible = isLoading
	}

	override fun onClick(v: View) {
		val manga = viewModel.manga.value
		when {
			v.id == R.id.imageView_favourite -> {
				FavouriteCategoriesDialog.show(childFragmentManager, manga ?: return)
			}
			v.id == R.id.button_read -> {
				startActivity(
					ReaderActivity.newIntent(
						context ?: return,
						manga ?: return,
						viewModel.readingHistory.value
					)
				)
			}
			v is Chip -> {
				when (val tag = v.tag) {
					is String -> MangaSearchSheet.show(
						activity?.supportFragmentManager
							?: childFragmentManager,
						manga?.source ?: return, tag
					)
				}
			}
		}
	}

	override fun onLongClick(v: View): Boolean {
		when (v.id) {
			R.id.button_read -> {
				if (viewModel.readingHistory.value == null) {
					return false
				}
				v.showPopupMenu(R.menu.popup_read) {
					when (it.itemId) {
						R.id.action_read -> {
							startActivity(
								ReaderActivity.newIntent(
									context ?: return@showPopupMenu false,
									viewModel.manga.value ?: return@showPopupMenu false
								)
							)
							true
						}
						else -> false
					}
				}
				return true
			}
			else -> return false
		}
	}
}