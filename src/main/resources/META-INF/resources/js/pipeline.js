const serialize_form = form => JSON.stringify(
    Array.from(new FormData(form).entries())
         .reduce((m, [ key, value ]) => Object.assign(m, { [key]: value }), {})
  );
  
  $('#piplelineRunForm').on('submit', function(event) {
    event.preventDefault();
    const data = {
      releaseBranch: document.getElementById('releaseBranch').value,
      userNameFFM: document.getElementById('userNameFFM').value,
      userPassword: document.getElementById('userPassword').value,
      groups: document.getElementById('groups').value,
      browser: document.getElementById('browser').value,
      url: document.getElementById('url').value,
      seleniumTestEmailList: document.getElementById('seleniumTestEmailList').value,
      logs: document.getElementById('logs').value,
      mvnArgs: document.getElementById('mvnArgs').value,
      pipelineRunName: document.getElementById('pipelineRunName').value
    };
    const jsonData = JSON.stringify(data);
    console.log(jsonData);
    $.ajax({
      type: 'POST',
      url: '/pipeline/tester-pipelines/startRun',
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
  
  const validation = new JustValidate('#piplelineRunForm');
  
  validation
    .addField('#releaseBranch', [
      {
        rule: 'required',
        errorMessage: 'Release Branch is required'
      }
    ])
    .addField('#userPassword', [
      {
        rule: 'required',
        errorMessage: 'User Password is required'
      }
    ])
    .addField('#groups', [
      {
        rule: 'required',
        errorMessage: 'Groups is required'
      }
    ])
    .addField('#browser', [
      {
        rule: 'required',
        errorMessage: 'Browser is required'
      }
    ])
    .addField('#url', [
      {
        rule: 'required',
        errorMessage: 'Url is required'
      }
    ])
    .addField('#seleniumTestEmailList', [
      {
        rule: 'required',
        errorMessage: 'Selenium Test Email List  is required'
      }
    ])
    .addField('#logs', [
        {
          rule: 'required',
          errorMessage: 'Logs  is required'
        }
      ])
      .addField('#mvnArgs', [
        {
          rule: 'required',
          errorMessage: 'Maven Args are required'
        }
      ])
      .addField('#pipelineRunName', [
        {
          rule: 'required',
          errorMessage: 'A Pipeline run name is required'
        }
      ])
    .addField('#userNameFFM', [
      {
        rule: 'required',
        errorMessage: 'User Name is required',
      }
    ]);
  
  /* Set the width of the side navigation to 250px */
  function openNav() {
    document.getElementById("mySidenav").style.width = "350px";
  }
  
  /* Set the width of the side navigation to 0 */
  function closeNav() {
    document.getElementById("mySidenav").style.width = "0";
  }
  
  /* Allows you to search the tables */
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
  
    /* Allows for extending of failures */
    addEventListener('DOMContentLoaded', function() { 
      var test1 = document.getElementById('test1'); 
      
      test1.addEventListener('click', function(e) {
         if (e.target.classList.contains('reveal-cell')){ 
            e.target.classList.toggle('expanded'); 
          } 
      });
      
      
     }); 
     
  