// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.codeInsight.inspections.shared

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.inspections.AbstractKotlinApplicableInspection
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.KotlinApplicabilityRange
import org.jetbrains.kotlin.idea.codeinsight.api.applicators.applicabilityRange
import org.jetbrains.kotlin.idea.codeinsights.impl.base.applicators.ApplicabilityRanges
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset

internal class InfixCallToOrdinaryInspection : AbstractKotlinApplicableInspection<KtBinaryExpression>() {
    override fun getProblemDescription(element: KtBinaryExpression) = KotlinBundle.message("replace.infix.call.with.ordinary.call")

    override fun apply(element: KtBinaryExpression, project: Project, updater: ModPsiUpdater) {
        convertInfixCallToOrdinary(element)
    }

    override fun getActionFamilyName() = KotlinBundle.message("replace.infix.call.with.ordinary.call")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor =
        binaryExpressionVisitor {
            visitTargetElement(it, holder, isOnTheFly)
        }

    override fun getActionName(element: KtBinaryExpression): String = KotlinBundle.message("replace.infix.call.with.ordinary.call")

    override fun getApplicabilityRange(): KotlinApplicabilityRange<KtBinaryExpression> = applicabilityRange {
        it.operationReference.textRangeInParent
    }

    override fun isApplicableByPsi(element: KtBinaryExpression): Boolean {
        return !(element.operationToken != KtTokens.IDENTIFIER || element.left == null || element.right == null)
    }
}

fun convertInfixCallToOrdinary(element: KtBinaryExpression): KtExpression {
    val argument = KtPsiUtil.safeDeparenthesize(element.right!!)
    val pattern = "$0.$1" + when (argument) {
        is KtLambdaExpression -> " $2:'{}'"
        else -> "($2)"
    }

    val replacement = KtPsiFactory(element.project).createExpressionByPattern(
        pattern,
        element.left!!,
        element.operationReference,
        argument
    )

    return element.replace(replacement) as KtExpression
}