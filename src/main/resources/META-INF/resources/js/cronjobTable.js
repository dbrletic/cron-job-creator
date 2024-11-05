
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
      document.getElementById('test4'),
      document.getElementById('test9')
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
    document.addEventListener('DOMContentLoaded', function() {
      var test1ElementExpand = document.querySelector('#test1');
      var test2ElementExpand = document.querySelector('#test2');
      var test3ElementExpand = document.querySelector('#test3');
      var test4ElementExpand = document.querySelector('#test4');
      var test9ElementExpand = document.querySelector('#test9');
 
      if(test1ElementExpand){
        var test1Expand = document.getElementById('test1'); 
        test1Expand.addEventListener('click', function(e) {
          if (e.target.classList.contains('reveal-cell')){ 
             e.target.classList.toggle('expanded'); 
           } 
        });
      }
      if(test2ElementExpand){
        var test2Expand = document.getElementById('test2'); 
        test2Expand.addEventListener('click', function(e) {
          if (e.target.classList.contains('reveal-cell')){ 
             e.target.classList.toggle('expanded'); 
           } 
        });
      }
      if(test3ElementExpand){
        var test3Expand = document.getElementById('test3'); 
        test3Expand.addEventListener('click', function(e) {
          if (e.target.classList.contains('reveal-cell')){ 
             e.target.classList.toggle('expanded'); 
           } 
        });
      }
      if(test4ElementExpand){
        var test4Expand = document.getElementById('test4'); 
        test4Expand.addEventListener('click', function(e) {
          if (e.target.classList.contains('reveal-cell')){ 
             e.target.classList.toggle('expanded'); 
           } 
        });
      }
      if(test9ElementExpand){
        var test4Expand = document.getElementById('test9'); 
        test9Expand.addEventListener('click', function(e) {
          if (e.target.classList.contains('reveal-cell')){ 
             e.target.classList.toggle('expanded'); 
           } 
        });
      }
     });     
     

    

     /* Setting DataTables on all the tables */
     $(document).ready( function () {
      //Select all the <h3> elments on the page
      var h3Elements = document.querySelectorAll('h3');

      //Looping through each element to capitalize the first Letter of the table name
      h3Elements.forEach(function(h3Elment){
        h3Elment.textContent = h3Elment.textContent.charAt(0).toUpperCase() + h3Elment.textContent.slice(1);
      });

      //Setting DataTables on all the tables 
      //Have to do it the stupid way since I could not get the ' to escape correctly when creating this in java 
      var test1Element = document.querySelector('#test1');
      var test2Element = document.querySelector('#test2');
      var test3Element = document.querySelector('#test3');
      var test4Element = document.querySelector('#test4');
      var test9Element = document.querySelector('#test9');

      if(test1Element){
        var test1 = new DataTable('#test1', {
          paging: false,
          autoWidth: false, 
          columnDefs:  [
            { "width": "25%", "targets": 0 },
            { "width": "7%", "targets": 1 },
            { "width": "21%", "targets": 2 },
            { "width": "20%", "targets": 3 },
            { "width": "7%", "targets": 4 },
            { "width": "8%", "targets": 5 },
            { "width": "12%", "targets": 6 }
        ]
          } );
      }
      if(test2Element){
          var test2 = new DataTable('#test2', {
            paging: false,
          autoWidth: false, 
          columnDefs:  [
            { "width": "25%", "targets": 0 },
            { "width": "7%", "targets": 1 },
            { "width": "21%", "targets": 2 },
            { "width": "20%", "targets": 3 },
            { "width": "7%", "targets": 4 },
            { "width": "8%", "targets": 5 },
            { "width": "12%", "targets": 6 }
        ]
          } );
      }
      if(test3Element){
          var test3 = new DataTable('#test3', {
            paging: false,
          autoWidth: false, 
          columnDefs:  [
            { "width": "25%", "targets": 0 },
            { "width": "7%", "targets": 1 },
            { "width": "21%", "targets": 2 },
            { "width": "20%", "targets": 3 },
            { "width": "7%", "targets": 4 },
            { "width": "8%", "targets": 5 },
            { "width": "12%", "targets": 6 }
        ]
          } );
      }
      if(test4Element){
          var test4 = new DataTable('#test4', {
            paging: false,
          autoWidth: false, 
          columnDefs:  [
            { "width": "25%", "targets": 0 },
            { "width": "7%", "targets": 1 },
            { "width": "21%", "targets": 2 },
            { "width": "20%", "targets": 3 },
            { "width": "7%", "targets": 4 },
            { "width": "8%", "targets": 5 },
            { "width": "12%", "targets": 6 }
        ]
          } );
      }
      if(test9Element){
          var test9 = new DataTable('#test9', {
            paging: false,
          autoWidth: false, 
          columnDefs:  [
            { "width": "25%", "targets": 0 },
            { "width": "7%", "targets": 1 },
            { "width": "21%", "targets": 2 },
            { "width": "20%", "targets": 3 },
            { "width": "7%", "targets": 4 },
            { "width": "8%", "targets": 5 },
            { "width": "12%", "targets": 6 }
        ]
          } );
      }
     });