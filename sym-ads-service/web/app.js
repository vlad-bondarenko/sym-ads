function includeHTML(id, url) {
    var elmnt, xhttp;
    elmnt = document.getElementById(id);
    xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState == 4) {
            if (this.status == 200) {
                elmnt.innerHTML = this.responseText;
            }
            if (this.status == 404) {
                elmnt.innerHTML = "Page not found.";
            }
        }
    };
    xhttp.open("GET", url, true);
    xhttp.send();
}