const serialize_form = form => JSON.stringify(
  Array.from(new FormData(form).entries())
       .reduce((m, [ key, value ]) => Object.assign(m, { [key]: value }), {})
);

$('#cronForum').on('submit', function(event) {
  event.preventDefault();
  const data = {
    releaseBranch: document.getElementById('releaseBranch').value,
    userNameFFM: document.getElementById('userNameFFM').value,
    userPassword: document.getElementById('userPassword').value,
    groups: document.getElementById('groups').value,
    browser: document.getElementById('browser').value,
    url: document.getElementById('url').value,
    seleniumTestEmailList: document.getElementById('seleniumTestEmailList').value,
    cronJobSchedule: document.getElementById('cronJobSchedule').value
  };
  const jsonData = JSON.stringify(data);
  console.log(jsonData);
  var host = window.location.hostname;
  console.log(host);
  $.ajax({
    type: 'POST',
    url: '/ffe-cronjob',
    data: jsonData,
    contentType: 'application/json',
    responseType: 'blob', // Set the response type to blob
    success: function(response, status, xhr) {
      $("#cronForum")[0].reset();
      let today = new Date().toISOString().slice(0, 10)
      var blob = new Blob([response], { type: "application/zip" });
      var link = document.createElement('a');
      link.href = window.URL.createObjectURL(blob);
      link.download = data.groups + "-" + data.url + "-" + today + ".zip";
      link.click();
    },
    error: function(xhr, status, error) {
      console.log("Error: " + error);
    }
  });
});