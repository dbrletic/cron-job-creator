/* const form = document.querySelector('form');
form.addEventListener('submit', event => {
  event.preventDefault();
  const data = {
    releaseBranch: document.getElementById('releaseBranch').value,
    userNameFFM: document.getElementById('userNameFFM').value,
    userPassword: document.getElementById('userPassword').value,
    groups: document.getElementById('groups').value,
    browser: document.getElementById('browser').value,
    url: document.getElementById('url').value,
    seleniumTestEmailList: document.getElementById('seleniumTestEmailList').value
  };
  fetch('http://localhost:8080/ffe-cronjob', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  })
    .then(response => response.json())
    .then(data => console.log(data));
}); */

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
  const json = JSON.stringify(data);
  console.log(json);
  $.ajax({
    type: 'POST',
    url: 'http://localhost:8080/ffe-cronjob',
    dataType: 'arraybuffer',
    data: json,
    contentType: 'application/json',
    success: function(response) {
      console.log("Success Response from POST")
      link.href = window.URL.createObjectURL(blob);
      link.download = 'zipTest.zip';
      link.click();
    }
  });
});