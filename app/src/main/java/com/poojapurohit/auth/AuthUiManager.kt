package com.poojapurohit.auth

import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView


class AuthUiManager(
    private val tvWelcome: TextView,
    private val tvNameLabel: TextView,
    private val tvPhoneLabel: TextView,
    private val tvLoc: TextView,
    private val tvSpec: TextView,
    private val tvExperienceLabel: TextView,
    private val etName: EditText,
    private val etPhone: EditText,
    private val etLoc: EditText,
    private val etExperience: EditText,
    private val btnNext: Button,
    private val btnRegister: Button,
    private val line1: View,
    private val line2: View,
    private val googleAuthButton: View,
    private val tvAreYouPurohit: TextView,
    private val tvRegisterHere: TextView,
    private val rvServices: RecyclerView,
    private val onNextClickListener: () -> Unit,
    private val onRegisterClickListener: () -> Unit
) {
    var currentStep = 0
    var isServicePartnerFlow = false

    init {
        btnNext.setOnClickListener { onNextClickListener() }
        btnRegister.setOnClickListener { onRegisterClickListener() }
    }

    fun showInitialState() {
        isServicePartnerFlow = false
        currentStep = 0
        hideAllFields()
        googleAuthButton.visibility = View.VISIBLE
        tvAreYouPurohit.visibility = View.VISIBLE
        tvRegisterHere.visibility = View.VISIBLE
    }

    fun showCustomerFields() {
        isServicePartnerFlow = false
        currentStep = 1
        hideAllFields()
        tvWelcome.visibility = View.VISIBLE
        tvNameLabel.visibility = View.VISIBLE
        line1.visibility = View.VISIBLE
        etName.visibility = View.VISIBLE
        tvPhoneLabel.visibility = View.VISIBLE
        line2.visibility = View.VISIBLE
        etPhone.visibility = View.VISIBLE
        btnRegister.visibility = View.VISIBLE
    }

    fun showServicePartnerStep1() {
        isServicePartnerFlow = true
        currentStep = 1
        hideAllFields()

        tvWelcome.visibility = View.VISIBLE

        tvNameLabel.visibility = View.VISIBLE
        line1.visibility = View.VISIBLE
        etName.visibility = View.VISIBLE
        etName.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS

        tvPhoneLabel.visibility = View.VISIBLE
        line2.visibility = View.VISIBLE
        etPhone.visibility = View.VISIBLE

        val parent = btnNext.parent as ConstraintLayout
        val set = ConstraintSet()
        set.clone(parent)
        set.clear(btnNext.id, ConstraintSet.TOP)
        set.connect(btnNext.id, ConstraintSet.TOP, etPhone.id, ConstraintSet.BOTTOM, 16)
        set.applyTo(parent)
        btnNext.visibility = View.VISIBLE

    }

    fun showServicePartnerStep2() {
        isServicePartnerFlow = true
        currentStep = 2
        hideAllFields()

        tvWelcome.visibility = View.VISIBLE
        tvLoc.visibility = View.VISIBLE
        line1.visibility = View.VISIBLE
        etLoc.visibility = View.VISIBLE
        etLoc.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS

        val parent = btnNext.parent as ConstraintLayout
        val set = ConstraintSet()
        set.clone(parent)

        // line1 below tvLoc
        set.clear(line1.id, ConstraintSet.TOP)
        set.connect(line1.id, ConstraintSet.TOP, tvLoc.id, ConstraintSet.BOTTOM, 8)

        // etLoc below line1
        set.clear(etLoc.id, ConstraintSet.TOP)
        set.connect(etLoc.id, ConstraintSet.TOP, line1.id, ConstraintSet.BOTTOM, 8)

        // btnNext below etLoc
        set.clear(btnNext.id, ConstraintSet.TOP)
        set.connect(btnNext.id, ConstraintSet.TOP, etLoc.id, ConstraintSet.BOTTOM, 16)

        set.applyTo(parent)

        btnNext.visibility = View.VISIBLE
    }


    fun showServicePartnerStep3() {
        isServicePartnerFlow = true
        currentStep = 3
        hideAllFields()

        val parent = btnRegister.parent as ConstraintLayout
        val set = ConstraintSet()
        set.clone(parent)

        set.clear(etExperience.id, ConstraintSet.TOP)
        set.connect(etExperience.id, ConstraintSet.TOP, rvServices.id, ConstraintSet.BOTTOM, 8)

        set.clear(btnRegister.id, ConstraintSet.TOP)
        set.connect(btnRegister.id, ConstraintSet.TOP, etExperience.id, ConstraintSet.BOTTOM, 16)

        set.applyTo(parent)

        tvWelcome.visibility = View.VISIBLE
        tvSpec.visibility = View.VISIBLE
        line1.visibility = View.VISIBLE
        rvServices.visibility = View.VISIBLE
        tvExperienceLabel.visibility = View.VISIBLE
        etExperience.visibility = View.VISIBLE
        etExperience.inputType = InputType.TYPE_CLASS_NUMBER
        btnRegister.visibility = View.VISIBLE
    }

    private fun hideAllFields() {
        tvWelcome.visibility = View.GONE
        tvNameLabel.visibility = View.GONE
        tvPhoneLabel.visibility = View.GONE
        tvLoc.visibility = View.GONE
        tvSpec.visibility = View.GONE
        tvExperienceLabel.visibility = View.GONE
        etName.visibility = View.GONE
        etPhone.visibility = View.GONE
        etLoc.visibility = View.GONE
        etExperience.visibility = View.GONE
        btnNext.visibility = View.GONE
        btnRegister.visibility = View.GONE
        rvServices.visibility = View.GONE
        googleAuthButton.visibility = View.GONE
        tvAreYouPurohit.visibility = View.GONE
        tvRegisterHere.visibility = View.GONE
        line1.visibility = View.GONE
        line2.visibility = View.GONE
    }

    /**
     * Handle back navigation between steps.
     * Returns true if handled (did navigate to previous UI step), false if not handled.
     */
    fun goBackToPreviousStep(): Boolean {
        return when (currentStep) {
            3 -> {
                showServicePartnerStep2()
                true
            }
            2 -> {
                showServicePartnerStep1()
                true
            }
            1 -> {
                // On step 1 (either customer or provider), go back to initial state
                showInitialState()
                true
            }
            else -> false
        }
    }
}
