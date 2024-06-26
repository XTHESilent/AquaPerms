/*
 * This file is part of AquaPerms, licensed under the MIT License.
 *
 *  Copyright (c) AquasplashMC (XTHESilent) <xthesilent@aquasplashmc.com>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.xthesilent.aquaperms.common.commands.misc;

import com.xthesilent.aquaperms.common.command.abstraction.SingleCommand;
import com.xthesilent.aquaperms.common.command.access.CommandPermission;
import com.xthesilent.aquaperms.common.command.spec.CommandSpec;
import com.xthesilent.aquaperms.common.command.utils.ArgumentList;
import com.xthesilent.aquaperms.common.http.UnsuccessfulRequestException;
import com.xthesilent.aquaperms.common.locale.Message;
import com.xthesilent.aquaperms.common.locale.TranslationManager;
import com.xthesilent.aquaperms.common.locale.TranslationRepository.LanguageInfo;
import com.xthesilent.aquaperms.common.plugin.AquaPermsPlugin;
import com.xthesilent.aquaperms.common.sender.Sender;
import com.xthesilent.aquaperms.common.util.Predicates;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TranslationsCommand extends SingleCommand {

    public TranslationsCommand() {
        super(CommandSpec.TRANSLATIONS, "Translations", CommandPermission.TRANSLATIONS, Predicates.notInRange(0, 1));
    }

    @Override
    public void execute(AquaPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        Message.TRANSLATIONS_SEARCHING.send(sender);

        List<LanguageInfo> availableTranslations;
        try {
            availableTranslations = plugin.getTranslationRepository().getAvailableLanguages();
        } catch (IOException | UnsuccessfulRequestException e) {
            Message.TRANSLATIONS_SEARCHING_ERROR.send(sender);
            plugin.getLogger().warn("Unable to obtain a list of available translations", e);
            return;
        }

        if (args.size() >= 1 && args.get(0).equalsIgnoreCase("install")) {
            Message.TRANSLATIONS_INSTALLING.send(sender);
            plugin.getTranslationRepository().downloadAndInstallTranslations(availableTranslations, sender, true);
            Message.TRANSLATIONS_INSTALL_COMPLETE.send(sender);
            return;
        }

        Message.INSTALLED_TRANSLATIONS.send(sender, plugin.getTranslationManager().getInstalledLocales().stream().map(Locale::toLanguageTag).sorted().collect(Collectors.toList()));

        Message.AVAILABLE_TRANSLATIONS_HEADER.send(sender);
        availableTranslations.stream()
                .sorted(Comparator.comparing(language -> language.locale().toLanguageTag()))
                .forEach(language -> Message.AVAILABLE_TRANSLATIONS_ENTRY.send(sender, language.locale().toLanguageTag(), TranslationManager.localeDisplayName(language.locale()), language.progress(), language.contributors()));
        sender.sendMessage(Message.prefixed(Component.empty()));
        Message.TRANSLATIONS_DOWNLOAD_PROMPT.send(sender, label);
    }

}
