(function () {
  'use strict';

  const spec = window.__API_SPEC__;
  if (!spec) {
    document.getElementById('app').textContent = 'Error: __API_SPEC__ not found.';
    return;
  }

  let globalBaseUrl = '';

  function el(tag, attrs, ...children) {
    const node = document.createElement(tag);
    if (attrs) Object.entries(attrs).forEach(([k, v]) => {
      if (k === 'class') node.className = v;
      else if (k === 'style') node.style.cssText = v;
      else if (k.startsWith('on')) node.addEventListener(k.slice(2), v);
      else node.setAttribute(k, v);
    });
    children.forEach(c => {
      if (c == null) return;
      node.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
    });
    return node;
  }

  function resolveRef(ref) {
    if (!ref || !ref.startsWith('#/components/schemas/')) return null;
    const name = ref.replace('#/components/schemas/', '');
    return (spec.components && spec.components.schemas && spec.components.schemas[name]) || null;
  }

  function renderSchemaText(schema, depth, visited) {
    if (!schema) return 'any';
    depth = depth || 0;
    visited = visited || new Set();
    var indent = '  '.repeat(depth);
    var childIndent = '  '.repeat(depth + 1);

    if (schema['$ref']) {
      var name = schema['$ref'].replace('#/components/schemas/', '');
      if (visited.has(name)) return '$ref(' + name + ') [circular]';
      visited.add(name);
      var resolved = resolveRef(schema['$ref']);
      if (resolved) return renderSchemaText(resolved, depth, visited);
      return '$ref(' + name + ')';
    }

    var type = schema.type || 'object';

    if (type === 'array') {
      var items = schema.items ? renderSchemaText(schema.items, 0, visited) : 'any';
      return 'Array<' + items + '>';
    }
    if (type === 'object') {
      if (schema.additionalProperties) {
        return 'Map<string, ' + renderSchemaText(schema.additionalProperties, 0, visited) + '>';
      }
      if (!schema.properties) return 'object';
      var lines = ['{'];
      for (var k in schema.properties) {
        if (!schema.properties.hasOwnProperty(k)) continue;
        var v = schema.properties[k];
        var req = schema.required && schema.required.includes(k) ? '' : '?';
        lines.push(childIndent + k + req + ': ' + renderSchemaText(v, depth + 1, visited));
      }
      lines.push(indent + '}');
      return lines.join('\n');
    }
    if (schema['enum']) return schema['enum'].join(' | ');
    if (schema.format) return type + '(' + schema.format + ')';
    return type;
  }

  function buildSampleBody(schema, visited) {
    if (!schema) return {};
    visited = visited || new Set();

    if (schema['$ref']) {
      var name = schema['$ref'].replace('#/components/schemas/', '');
      if (visited.has(name)) return '<' + name + '>';
      visited.add(name);
      var resolved = resolveRef(schema['$ref']);
      return resolved ? buildSampleBody(resolved, visited) : {};
    }

    var type = schema.type || 'object';
    if (type === 'string') return schema['enum'] ? schema['enum'][0] : (schema.format === 'date-time' ? '2024-01-01T00:00:00Z' : 'string');
    if (type === 'integer' || type === 'number') return 0;
    if (type === 'boolean') return false;
    if (type === 'array') return [buildSampleBody(schema.items, visited)];
    if (type === 'object') {
      if (schema.additionalProperties) return { key: buildSampleBody(schema.additionalProperties, visited) };
      if (!schema.properties) return {};
      var obj = {};
      for (var k in schema.properties) {
        if (!schema.properties.hasOwnProperty(k)) continue;
        obj[k] = buildSampleBody(schema.properties[k], visited);
      }
      return obj;
    }
    return null;
  }

  function methodClass(method) {
    const classes = {
      'get': 'dg-method-GET',
      'post': 'dg-method-POST',
      'put': 'dg-method-PUT',
      'delete': 'dg-method-DELETE',
      'patch': 'dg-method-PATCH'
    };
    return classes[method.toLowerCase()] || 'dg-method-default';
  }

  function renderEndpoint(path, method, operation) {
    var card = el('div', { 'class': 'dg-endpoint' });

    var header = el('div', { 'class': 'dg-endpoint-header' },
      el('span', { 'class': methodClass(method) }, method.toUpperCase()),
      el('span', { 'class': 'dg-path' }, path),
      el('span', { 'class': 'dg-summary' }, operation.summary || operation.operationId || ''),
      el('span', { 'class': 'dg-chevron' }, '\u25B6')
    );
    header.addEventListener('click', function() { card.classList.toggle('open'); });
    card.appendChild(header);

    var body = el('div', { 'class': 'dg-endpoint-body' });

    var params = operation.parameters;
    if (params && params.length > 0) {
      var section = el('div', { 'class': 'dg-section' },
        el('div', { 'class': 'dg-section-title' }, 'Parameters')
      );
      var table = el('table', { 'class': 'dg-params-table' },
        el('thead', {},
          el('tr', {},
            el('th', {}, 'Name'),
            el('th', {}, 'In'),
            el('th', {}, 'Type'),
            el('th', {}, 'Required')
          )
        )
      );
      var tbody = el('tbody', {});
      params.forEach(function(p) {
        var typeText = p.schema ? renderSchemaText(p.schema) : 'any';
        tbody.appendChild(el('tr', {},
          el('td', {}, el('code', {}, p.name)),
          el('td', {}, el('span', { 'class': 'dg-in-badge' }, p['in'] || '')),
          el('td', {}, el('code', {}, typeText)),
          el('td', {}, p.required
            ? el('span', { 'class': 'dg-badge-required' }, '\u2713 required')
            : el('span', { 'class': 'dg-badge-optional' }, 'optional')
          )
        ));
      });
      table.appendChild(tbody);
      section.appendChild(table);
      body.appendChild(section);
    }

    if (operation.requestBody && operation.requestBody.content) {
      var ct = Object.values(operation.requestBody.content)[0];
      if (ct && ct.schema) {
        var schemaText = renderSchemaText(ct.schema);
        var sample = JSON.stringify(buildSampleBody(ct.schema), null, 2);
        var rbSection = el('div', { 'class': 'dg-section' },
          el('div', { 'class': 'dg-section-title' }, 'Request Body'),
          el('pre', { 'class': 'dg-schema' }, schemaText),
          el('div', { 'class': 'dg-section-title', style: 'margin-top:.5rem' }, 'Example'),
          el('pre', { 'class': 'dg-schema' }, sample)
        );
        body.appendChild(rbSection);
      }
    }

    if (operation.responses) {
      var respSection = el('div', { 'class': 'dg-section' },
        el('div', { 'class': 'dg-section-title' }, 'Responses')
      );
      for (var code in operation.responses) {
        if (!operation.responses.hasOwnProperty(code)) continue;
        var resp = operation.responses[code];
        var desc = resp.description || '';
        var rsText = '';
        if (resp.content) {
          var rCt = Object.values(resp.content)[0];
          if (rCt && rCt.schema) rsText = renderSchemaText(rCt.schema);
        }
        respSection.appendChild(el('div', {},
          el('strong', {}, code + ' '),
          el('span', {}, desc),
          rsText ? el('pre', { 'class': 'dg-schema', style: 'margin-top:.35rem' }, rsText) : null
        ));
      }
      body.appendChild(respSection);
    }

    body.appendChild(renderTryItOut(path, method, operation));

    card.appendChild(body);
    return card;
  }

  function renderTryItOut(path, method, operation) {
    var box = el('div', { 'class': 'dg-try' },
      el('div', { 'class': 'dg-try-title' }, '\u26A1 Try it out')
    );

    var params = operation.parameters || [];
    var pathParams   = params.filter(function(p) { return p['in'] === 'path'; });
    var queryParams  = params.filter(function(p) { return p['in'] === 'query'; });
    var headerParams = params.filter(function(p) { return p['in'] === 'header'; });

    var inputMap = {};

    function addRow(label, name) {
      var input = el('input', { type: 'text', placeholder: name });
      inputMap[name] = input;
      return el('div', { 'class': 'dg-try-row' }, el('label', {}, label), input);
    }

    pathParams.forEach(function(p) { box.appendChild(addRow('{' + p.name + '}', p.name)); });
    queryParams.forEach(function(p) { box.appendChild(addRow(p.name + ' (query)', p.name)); });
    headerParams.forEach(function(p) { box.appendChild(addRow(p.name + ' (header)', p.name)); });

    var bodyInput = null;
    if (operation.requestBody && operation.requestBody.content) {
      var ct = Object.values(operation.requestBody.content)[0];
      var sample = ct && ct.schema ? JSON.stringify(buildSampleBody(ct.schema), null, 2) : '{}';
      bodyInput = el('textarea', { placeholder: 'Request body (JSON)' }, sample);
      box.appendChild(el('div', { 'class': 'dg-try-row' }, el('label', {}, 'Body (JSON)'), bodyInput));
    }

    var execBtn = el('button', { 'class': 'dg-execute-btn' }, 'Execute');
    box.appendChild(execBtn);

    var responseBox = el('div', { 'class': 'dg-response-box' });
    box.appendChild(responseBox);

    execBtn.addEventListener('click', function() {
      responseBox.innerHTML = '';

      var baseUrl = globalBaseUrl.replace(/\/$/, '');
      var url = path;

      pathParams.forEach(function(p) {
        var val = inputMap[p.name] ? inputMap[p.name].value : '';
        url = url.replace('{' + p.name + '}', encodeURIComponent(val));
      });

      var qs = queryParams
        .map(function(p) {
          var val = inputMap[p.name] ? inputMap[p.name].value : '';
          return val ? encodeURIComponent(p.name) + '=' + encodeURIComponent(val) : '';
        })
        .filter(Boolean)
        .join('&');
      if (qs) url += '?' + qs;

      var fullUrl = baseUrl + url;

      var headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
      headerParams.forEach(function(p) {
        var val = inputMap[p.name] ? inputMap[p.name].value : '';
        if (val) headers[p.name] = val;
      });

      var fetchOpts = {
        method: method.toUpperCase(),
        headers: headers,
        mode: 'cors'
      };

      if (bodyInput && bodyInput.value.trim()) {
        fetchOpts.body = bodyInput.value;
      }

      var statusDiv = el('div', { 'class': 'dg-response-status' });
      var bodyPre   = el('pre', { 'class': 'dg-response-body' }, 'Loading\u2026');
      responseBox.appendChild(statusDiv);
      responseBox.appendChild(bodyPre);

      fetch(fullUrl, fetchOpts)
        .then(function(resp) {
          return resp.text().then(function(text) {
            statusDiv.textContent = 'HTTP ' + resp.status + ' ' + resp.statusText;
            statusDiv.className = 'dg-response-status ' + (resp.ok ? 'ok' : 'err');
            var pretty = text;
            try { pretty = JSON.stringify(JSON.parse(text), null, 2); } catch(e) {}
            bodyPre.textContent = pretty;
          });
        })
        .catch(function(err) {
          statusDiv.textContent = 'Network Error: ' + err.message;
          statusDiv.className = 'dg-response-status err';
          bodyPre.textContent = 'Failed to fetch. Check CORS or server availability.\n\n' + err.message;
        });
    });

    return box;
  }

  function renderGlobalControls() {
    var defaultUrl = (spec.servers && spec.servers[0] && spec.servers[0].url) || 'http://localhost:8080';
    globalBaseUrl = defaultUrl;

    var container = el('div', {
      'class': 'dg-global-controls',
      'style': 'background: #fff; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,.08); margin-bottom: 2rem; padding: 1rem;'
    });

    var title = el('div', {
      'class': 'dg-section-title',
      'style': 'margin-bottom: 0.75rem;'
    }, '🌐 Server Configuration');

    var row = el('div', { 'class': 'dg-try-row', 'style': 'margin-bottom: 0;' },
      el('label', { 'style': 'min-width: 80px;' }, 'Base URL:'),
      el('input', {
        type: 'text',
        value: defaultUrl,
        style: 'flex: 1; padding: 0.5rem; border: 1px solid #cbd5e0; border-radius: 5px; font-size: 0.9rem;',
        oninput: function(e) {
          globalBaseUrl = e.target.value;
        }
      })
    );

    var hint = el('div', {
      'style': 'font-size: 0.75rem; color: #718096; margin-top: 0.5rem;'
    }, 'This URL will be used for all API requests');

    container.appendChild(title);
    container.appendChild(row);
    container.appendChild(hint);

    return container;
  }

  function renderHeader() {
    var info = spec.info || {};
    var server = (spec.servers && spec.servers[0] && spec.servers[0].url) || '';
    return el('header', { 'id': 'dg-header' },
      el('h1', {}, info.title || 'API Documentation'),
      el('span', { 'class': 'dg-version' }, 'v' + (info.version || '1.0')),
      el('span', { 'class': 'dg-server' }, server)
    );
  }

  function render() {
    var app = document.getElementById('app');
    app.appendChild(renderHeader());

    var main = el('div', { id: 'dg-main' });

    main.appendChild(renderGlobalControls());

    var byTag = {};
    if (spec.paths) {
      for (var path in spec.paths) {
        if (!spec.paths.hasOwnProperty(path)) continue;
        var pathItem = spec.paths[path];
        for (var method in pathItem) {
          if (!pathItem.hasOwnProperty(method)) continue;
          var operation = pathItem[method];
          if (typeof operation !== 'object' || !operation) continue;
          var tags = (operation.tags && operation.tags.length > 0) ? operation.tags : ['default'];
          tags.forEach(function(tag) {
            if (!byTag[tag]) byTag[tag] = [];
            byTag[tag].push({ path: path, method: method, operation: operation });
          });
        }
      }
    }

    for (var tag in byTag) {
      if (!byTag.hasOwnProperty(tag)) continue;
      var group = el('div', { 'class': 'dg-tag-group' },
        el('div', { 'class': 'dg-tag-title' }, tag)
      );
      byTag[tag].forEach(function(entry) {
        group.appendChild(renderEndpoint(entry.path, entry.method, entry.operation));
      });
      main.appendChild(group);
    }

    app.appendChild(main);
    app.appendChild(el('footer', { id: 'dg-footer' }, 'Generated by Doc-Gen \u2022 ' + new Date().getFullYear()));
  }

  document.addEventListener('DOMContentLoaded', render);
})();