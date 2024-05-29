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

function searchTables() {
  const searchInput = document.getElementById('searchInput');
  const searchValue = searchInput.value.trim();
  const rowIndex = 0; // Search in the first column
  const tables = [
    document.getElementById('test1'),
    document.getElementById('test2'),
    document.getElementById('test3'),
    document.getElementById('test4')
  ];

  tables.forEach(table => {
    const rows = table.rows;
    const searchRegex = new RegExp(searchValue, 'i'); // Create a case-insensitive regular expression

    // Iterate over each row in the table
    for (let i = 1; i < rows.length; i++) { // Skip the header row
      const row = rows[i];
      const cell = row.cells[rowIndex];
      const cellText = cell.textContent.trim();

      // Check if the cell in the specified row contains the search value
      if (searchRegex.test(cellText)) {
        row.style.display = ''; // Show the row
      } else {
        row.style.display = 'none'; // Hide the row
      }
    }
  });
}