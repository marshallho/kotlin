/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtSymbolDeclarationOverridesProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol

internal class KtFirSymbolDeclarationOverridesProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtSymbolDeclarationOverridesProvider(), KtFirAnalysisSessionComponent {

    override fun getOverriddenSymbols(
        callableSymbol: KtCallableSymbol,
        containingDeclaration: KtClassOrObjectSymbol
    ): List<KtFunctionSymbol> {

        check(callableSymbol is KtFirFunctionSymbol)
        check(containingDeclaration is KtFirClassOrObjectSymbol)

        return callableSymbol.firRef.withFir(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) { firFunction ->

            containingDeclaration.firRef.withFir(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) { containingDeclaration ->

                val firTypeScope = containingDeclaration.unsubstitutedScope(
                    containingDeclaration.session,
                    ScopeSession()
                )

                val overriddenFunctions = mutableListOf<KtFunctionSymbol>()
                firTypeScope.processFunctionsByName(firFunction.name) { }
                firTypeScope.processOverriddenFunctions(firFunction.symbol) { overriddenDeclaration ->

                    val symbolAsSimpleFunction = overriddenDeclaration.fir as? FirSimpleFunction //TODO
                    if (symbolAsSimpleFunction != null) {
                        val ktSymbol = analysisSession.firSymbolBuilder.buildFunctionSymbol(symbolAsSimpleFunction)
                        overriddenFunctions.add(ktSymbol)
                    }
                    ProcessorAction.NEXT
                }

                overriddenFunctions
            }
        }
    }
}