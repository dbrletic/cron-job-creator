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
      $('#errorMessage').empty();
      $("#successMessage").show();      
    },
    error: function(xhr, status, error) {
      $("#successMessage").hide();     
      console.log("Error: " + error);
      $("#cronForum")[0].reset();
      $("<p>Error: " + error +"</p>").appendTo('#errorMessage');
    }
  });
});

/* Set the width of the side navigation to 250px */
function openNav() {
  document.getElementById("mySidenav").style.width = "350px";
}

/* Set the width of the side navigation to 0 */
function closeNav() {
  document.getElementById("mySidenav").style.width = "0";
}

function filterCronJobs() {
  // Declare variables
  var input, filter, ul, li, a, i, txtValue;
  input = document.getElementById('cronJobFilter');
  filter = input.value.toUpperCase();
  ul = document.getElementById("cronJobData");
  li = ul.getElementsByTagName('li');

  // Loop through all list items, and hide those who don't match the search query
  for (i = 0; i < li.length; i++) {
    a = li[i].getElementsByTagName("a")[0];
    txtValue = a.textContent || a.innerText;
    if (txtValue.toUpperCase().indexOf(filter) > -1) {
      li[i].style.display = "";
    } else {
      li[i].style.display = "none";
    }
  }
}