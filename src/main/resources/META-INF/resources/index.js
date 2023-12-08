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
  $.ajax({
    type: 'POST',
    url: '/ffe-cronjob',
    data: jsonData,
    contentType: 'application/json',
    xhrFields:{
      responseType: 'blob'
    },
    success: function(response) {
      $("#cronForum")[0].reset();
      var link = document.createElement('a');
      let today = new Date().toISOString().slice(0, 10)
            link.href = window.URL.createObjectURL(response)
            link.download = data.groups + "-" + data.url + "-" + today + ".zip";
            document.body.appendChild(link);
            link.click();
            
    },
    error: function(xhr, status, error) {
      console.log("Error: " + error);
      $("#cronForum")[0].reset();
      
    }
  });
});