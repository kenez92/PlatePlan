(function () {
    const GENERATE_ID = "generate-plan";
    const ERROR_ID = "plan-error";
    const STATUS_ID = "plan-status";
    const DOWNLOADS_ID = "plan-downloads";
    const DIET_ID = "download-diet";
    const LIST_ID = "download-list";
    const URL_ATTRIBUTE = "data-generate-url";
    const TOKEN_ATTRIBUTE = "data-csrf-token";
    const HEADER_ATTRIBUTE = "data-csrf-header";
    const MSG_PROFILE = "data-msg-profile";
    const MSG_CALORIES = "data-msg-calories";
    const MSG_UNAVAILABLE = "data-msg-unavailable";
    const ERROR_PROFILE = "PROFILE_REQUIRED";
    const ERROR_CALORIES = "CALORIES_REQUIRED";
    const WAITING = "To może potrwać do dwóch minut.";
    const READY = "Plan gotowy. Pobierz oba pliki — po odświeżeniu strony znikną.";
    const PDF_TYPE = "application/pdf";

    const generateButton = document.getElementById(GENERATE_ID);
    if (generateButton === null) {
        return;
    }
    const panel = generateButton.closest("section");
    const errorBox = document.getElementById(ERROR_ID);
    const statusBox = document.getElementById(STATUS_ID);
    const downloads = document.getElementById(DOWNLOADS_ID);
    const dietLink = document.getElementById(DIET_ID);
    const listLink = document.getElementById(LIST_ID);
    let dietUrl = "";
    let listUrl = "";

    generateButton.addEventListener("click", generate);

    function generate() {
        hideError();
        hideDownloads();
        generateButton.disabled = true;
        statusBox.textContent = WAITING;
        fetch(generateButton.getAttribute(URL_ATTRIBUTE), {
            method: "POST",
            headers: {
                Accept: "application/json",
                [generateButton.getAttribute(HEADER_ATTRIBUTE)]: generateButton.getAttribute(TOKEN_ATTRIBUTE)
            }
        }).then(function (response) {
            if (!response.ok) {
                throw new Error();
            }
            return response.json();
        }).then(showResult).catch(function () {
            showError(panel.getAttribute(MSG_UNAVAILABLE));
        }).finally(function () {
            generateButton.disabled = false;
        });
    }

    function showResult(body) {
        if (body.error === ERROR_PROFILE) {
            showError(panel.getAttribute(MSG_PROFILE));
            return;
        }
        if (body.error === ERROR_CALORIES) {
            showError(panel.getAttribute(MSG_CALORIES));
            return;
        }
        if (body.error || !body.dietPdf || !body.shoppingListPdf) {
            showError(panel.getAttribute(MSG_UNAVAILABLE));
            return;
        }
        dietUrl = replaceUrl(dietUrl, dietLink, body.dietPdf);
        listUrl = replaceUrl(listUrl, listLink, body.shoppingListPdf);
        downloads.hidden = false;
        statusBox.textContent = READY;
    }

    function replaceUrl(previous, link, base64) {
        if (previous !== "") {
            URL.revokeObjectURL(previous);
        }
        const bytes = Uint8Array.from(atob(base64), function (character) {
            return character.charCodeAt(0);
        });
        const next = URL.createObjectURL(new Blob([bytes], { type: PDF_TYPE }));
        link.href = next;
        return next;
    }

    function showError(message) {
        statusBox.textContent = "";
        errorBox.textContent = message;
        errorBox.hidden = false;
        hideDownloads();
    }

    function hideError() {
        errorBox.textContent = "";
        errorBox.hidden = true;
    }

    function hideDownloads() {
        downloads.hidden = true;
        dietLink.removeAttribute("href");
        listLink.removeAttribute("href");
        if (dietUrl !== "") {
            URL.revokeObjectURL(dietUrl);
            dietUrl = "";
        }
        if (listUrl !== "") {
            URL.revokeObjectURL(listUrl);
            listUrl = "";
        }
    }
})();
